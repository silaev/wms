package com.silaev.wms.dao.impl;

import com.monitorjbl.xlsx.StreamingReader;
import com.silaev.wms.converter.ProductDtoToProductConverter;
import com.silaev.wms.dao.ExcelFileDao;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.exception.UploadProductException;
import com.silaev.wms.model.FileProductFieldAttributes;
import com.silaev.wms.model.FileRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Konstantin Silaev on 6/20/2020
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ExcelFileDaoImpl implements ExcelFileDao {
  public static final int ROW_CACHE_SIZE = 100;
  public static final int BUFFER_SIZE = 4096;
  private static final String XLSX_FILE_FORMAT = "xlsx";
  private final ProductDtoToProductConverter productConverter;

  public void verifyFileAttributes(final File file) {
    final String fileName = file.getName();
    final String absolutePath = file.getAbsolutePath();
    if (!FilenameUtils.isExtension(absolutePath, XLSX_FILE_FORMAT)) {
      throw new UploadProductException(
        String.format("The file: %s is supposed to be of xlsx format. " +
          "The Others are not currently supported", fileName)
      );
    }
  }

  public void verifyExcelFileHeader(final String fileName, final Row currentRow) {
    if (!FileProductFieldAttributes.ARTICLE.getFileColumnName().equalsIgnoreCase(getCell(currentRow, 0))
      || (!FileProductFieldAttributes.NAME.getFileColumnName().equalsIgnoreCase(getCell(currentRow, 1)))
      || (!FileProductFieldAttributes.QUANTITY.getFileColumnName().equalsIgnoreCase(getCell(currentRow, 2)))
      || (!FileProductFieldAttributes.SIZE.getFileColumnName().equalsIgnoreCase(getCell(currentRow, 3)))) {

      throw new UploadProductException(
        String.format(
          "File: %s has an incorrect header. Expected: %s %s %s %s",
          fileName,
          FileProductFieldAttributes.ARTICLE.getFileColumnName(),
          FileProductFieldAttributes.NAME.getFileColumnName(),
          FileProductFieldAttributes.QUANTITY.getFileColumnName(),
          FileProductFieldAttributes.SIZE.getFileColumnName()
        )
      );
    }
  }

  public String getCell(final Row currentRow, final int columnNumber) {
    String result = "";

    Cell cell = currentRow.getCell(columnNumber);
    if (cell.getCellType() == CellType.NUMERIC) {
      DecimalFormat decimalFormat = new DecimalFormat("#.##");
      decimalFormat.setRoundingMode(RoundingMode.CEILING);

      result = decimalFormat.format(cell.getNumericCellValue());
    } else if (cell.getCellType() == CellType.STRING) {

      result = cell.getStringCellValue().trim();
    } else if (cell.getCellType() == CellType.BOOLEAN) {
      result = String.valueOf(cell.getBooleanCellValue());
    }

    return result;
  }

  @Override
  public Flux<Product> getProducts(
    final String pathToStorage,
    final String fileName,
    final String userName
  ) {
    return Flux.defer(() -> {
      FileInputStream is;
      Workbook workbook;
      try {
        final File file = Paths.get(pathToStorage, userName, fileName).toFile();
        verifyFileAttributes(file);
        is = new FileInputStream(file);
        workbook = StreamingReader.builder()
          .rowCacheSize(ROW_CACHE_SIZE)
          .bufferSize(BUFFER_SIZE)
          .open(is);
      } catch (IOException e) {
        return Mono.error(new UploadProductException(
          String.format("An exception has been occurred while parsing a file: %s " +
            "has been saved", fileName), e));
      }

      try {
        final Sheet datatypeSheet = workbook.getSheetAt(0);
        final Iterator<Row> iterator = datatypeSheet.iterator();

        final AtomicInteger rowCounter = new AtomicInteger();
        if (iterator.hasNext()) {
          final Row currentRow = iterator.next();
          rowCounter.incrementAndGet();
          verifyExcelFileHeader(fileName, currentRow);
        }
        return Flux.<Product>create(fluxSink -> fluxSink.onRequest(value -> {
          try {
            for (int i = 0; i < value; i++) {
              if (!iterator.hasNext()) {
                fluxSink.complete();
                return;
              }

              final Row currentRow = iterator.next();
              final Product product = Objects.requireNonNull(getProduct(
                FileRow.builder()
                  .fileName(fileName)
                  .currentRow(currentRow)
                  .rowCounter(rowCounter.incrementAndGet())
                  .build()
              ), "product is not supposed to be null");
              fluxSink.next(product);
            }
          } catch (Exception e1) {
            fluxSink.error(e1);
          }
        })).doFinally(signalType -> {
          try {
            is.close();
            workbook.close();
          } catch (IOException e1) {
            log.error("Error has occurred while releasing {} resources: {}", fileName, e1);
          }
        });
      } catch (Exception e) {
        return Mono.error(e);
      }
    });
  }

  private Product getProduct(FileRow fileRow) {
    final Row currentRow = fileRow.getCurrentRow();
    final String fileName = fileRow.getFileName();
    final int rowCounter = fileRow.getRowCounter();
    Long article = Long.valueOf(getCell(currentRow, 0));
    if (Long.valueOf(0).equals(article)) {
      throwUploadProductException(fileName, rowCounter, FileProductFieldAttributes.ARTICLE.getFileColumnName());
    }
    String name = getCell(currentRow, 1);
    if (StringUtils.isEmpty(article)) {
      throwUploadProductException(fileName, rowCounter, FileProductFieldAttributes.NAME.getFileColumnName());
    }
    BigInteger quantity = new BigInteger(getCell(currentRow, 2));
    if (BigInteger.ZERO.equals(quantity)) {
      throwUploadProductException(fileName, rowCounter, FileProductFieldAttributes.QUANTITY.getFileColumnName());
    }

    final int sizeInteger = Integer.parseInt(getCell(currentRow, 3));
    if (sizeInteger == 0) {
      throwUploadProductException(fileName, rowCounter, FileProductFieldAttributes.SIZE.getFileColumnName());
    }

    ProductDto productDto = ProductDto.builder()
      .article(article)
      .name(name)
      .quantity(quantity)
      .size(sizeInteger)
      .build();

    return productConverter.convert(productDto);
  }

  private void throwUploadProductException(
    final String fileName,
    final int rowCounter,
    final String quantity
  ) {
    throw new UploadProductException(
      String.format(
        "File: %s, an empty %s in a row: %d ",
        fileName, quantity, rowCounter
      )
    );
  }
}
