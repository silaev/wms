package com.silaev.wms.service.impl;

import com.mongodb.client.model.UpdateOneModel;
import com.silaev.wms.converter.ProductAndUserNameToUpdateOneModelConverter;
import com.silaev.wms.dao.ExcelFileDao;
import com.silaev.wms.dao.ProductDao;
import com.silaev.wms.dao.ProductPatcherDao;
import com.silaev.wms.dto.FileUploadDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.exception.UploadProductException;
import com.silaev.wms.model.ProductArticleSizeDto;
import com.silaev.wms.service.UploadProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadProductServiceImpl implements UploadProductService {
  public static final int FILE_SIZE_THRESHOLD = 100000;
  private final ProductPatcherDao productPatcherService;
  private final ExcelFileDao excelFileDao;
  private final ProductDao productDao;
  private final ProductAndUserNameToUpdateOneModelConverter updateOneModelConverter;

  @Value("${storage.bulk-upload-path}")
  private String pathToStorage;

  public Flux<FileUploadDto> patchProductQuantity(
    final Flux<FilePart> files,
    final String userName
  ) {
    return Mono.fromRunnable(() -> initRootDirectory(userName))
      .publishOn(Schedulers.newBoundedElastic(1, 1, "initRootDirectory"))
      .log(String.format("cleaning-up directory: %s", userName))
      .thenMany(files.flatMap(f ->
          saveFileToDiskAndUpdate(f, userName)
            .subscribeOn(Schedulers.boundedElastic())
        )
      );
  }

  /**
   * Persists a single file coming from Flux<FilePart>
   * to a disk via transferTo method. After it,
   * parses and update the quantity of Products as
   * per Excel files
   *
   * @param file
   * @return
   */
  private Mono<FileUploadDto> saveFileToDiskAndUpdate(
    final FilePart file,
    final String userName
  ) {
    final String fileName = file.filename();
    final Path path = Paths.get(pathToStorage, userName, fileName);
    return Mono.just(path)
      .log(String.format("A file: %s has been uploaded", fileName))
      .flatMap(file::transferTo)
      .log(String.format("A file: %s has been saved", fileName))
      .then(processExcelFile(fileName, userName, path));
  }

  private Mono<FileUploadDto> processExcelFile(
    final String fileName,
    final String userName,
    final Path path
  ) {
    return Mono.fromCallable(() -> Files.size(path))
      .flatMap(size -> {
        if (size > FILE_SIZE_THRESHOLD) {
          return processBigExcelFile(fileName, userName);
        } else {
          return processSmallExcelFile(fileName, userName);
        }
      });
  }

  private Mono<FileUploadDto> processSmallExcelFile(
    final String fileName,
    final String userName
  ) {
    log.debug("processSmallExcelFile: {}", fileName);
    return excelFileDao.getProducts(pathToStorage, fileName, userName)
      .reduce(new ConcurrentHashMap<ProductArticleSizeDto, Tuple2<UpdateOneModel<Document>, BigInteger>>(),
        (indexMap, product) -> {
          final BigInteger quantity = product.getQuantity();
          indexMap.merge(
            new ProductArticleSizeDto(product.getArticle(), product.getSize()),
            Tuples.of(
              updateOneModelConverter.convert(Tuples.of(product, quantity, userName)),
              quantity
            ),
            (oldValue, newValue) -> {
              final BigInteger mergedQuantity = oldValue.getT2().add(newValue.getT2());
              return Tuples.of(
                updateOneModelConverter.convert(Tuples.of(product, mergedQuantity, userName)),
                mergedQuantity
              );
            }

          );
          return indexMap;
        })
      .filterWhen(productIndexFile ->
        productDao.findByArticleIn(extractArticles(productIndexFile.keySet()))
          .<ProductArticleSizeDto>handle(
            (productArticleSizeDto, synchronousSink) -> {
              if (productIndexFile.containsKey(productArticleSizeDto)) {
                synchronousSink.next(productArticleSizeDto);
              } else {
                synchronousSink.error(new UploadProductException(
                  String.format(
                    "A file %s does not have an article: %d with size: %s",
                    fileName,
                    productArticleSizeDto.getArticle(),
                    productArticleSizeDto.getSize()
                  )
                ));
              }
            })
          .count()
          .handle((sizeDb, synchronousSink) -> {
            final int sizeFile = productIndexFile.size();
            if (sizeDb == sizeFile) {
              synchronousSink.next(Boolean.TRUE);
            } else {
              synchronousSink.error(new UploadProductException(
                String.format(
                  "Inconsistency between total element size in MongoDB: %d and a file %s: %d",
                  sizeDb,
                  fileName,
                  sizeFile
                )
              ));
            }
          })
      ).onErrorResume(e -> {
        log.debug("Exception while processExcelFile fileName: {}: {}", fileName, e);
        return Mono.empty();
      }).flatMap(productIndexFile ->
        productPatcherService.incrementProductQuantity(
          fileName,
          productIndexFile.values().stream().map(Tuple2::getT1).collect(Collectors.toList()),
          userName
        )
      ).map(bulkWriteResult -> FileUploadDto.builder()
        .fileName(fileName)
        .matchedCount(bulkWriteResult.getMatchedCount())
        .modifiedCount(bulkWriteResult.getModifiedCount())
        .build()
      );
  }

  private Mono<FileUploadDto> processBigExcelFile(
    final String fileName,
    final String userName
  ) {
    log.debug("processBigExcelFile: {}", fileName);
    return excelFileDao.getProducts(pathToStorage, fileName, userName)
      .reduce(new ConcurrentHashMap<Product, Tuple2<UpdateOneModel<Document>, BigInteger>>(),
        (indexMap, product) -> {
          final BigInteger quantity = product.getQuantity();
          indexMap.merge(
            product,
            Tuples.of(
              updateOneModelConverter.convert(Tuples.of(product, quantity, userName)),
              quantity
            ),
            (oldValue, newValue) -> {
              final BigInteger mergedQuantity = oldValue.getT2().add(newValue.getT2());
              return Tuples.of(
                updateOneModelConverter.convert(Tuples.of(product, mergedQuantity, userName)),
                mergedQuantity
              );
            }

          );
          return indexMap;
        })
      .map(indexMap -> indexMap.values().stream().map(Tuple2::getT1).collect(Collectors.toList()))
      .onErrorResume(e -> {
        log.debug("Exception while processExcelFile: {}: {}", fileName, e);
        return Mono.empty();
      }).flatMap(dtoList ->
        productPatcherService.incrementProductQuantity(
          fileName,
          dtoList,
          userName
        )
      ).map(bulkWriteResult -> FileUploadDto.builder()
        .fileName(fileName)
        .matchedCount(bulkWriteResult.getMatchedCount())
        .modifiedCount(bulkWriteResult.getModifiedCount())
        .build()
      );
  }

  private List<Long> extractArticles(final Set<ProductArticleSizeDto> articleSizeDtos) {
    return articleSizeDtos.stream()
      .map(ProductArticleSizeDto::getArticle)
      .collect(Collectors.toList());
  }

  private void initRootDirectory(String userName) {
    final Path rootLocation = Paths.get(pathToStorage, userName);

    //clean up the root directory
    FileSystemUtils.deleteRecursively(rootLocation.toFile());

    //init the root directory
    try {
      Files.createDirectories(rootLocation);
    } catch (IOException e) {
      throw new UploadProductException(
        String.format(
          "An error while creation the root directory for: %s",
          userName
        )
      );
    }
  }
}
