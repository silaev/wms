package com.silaev.wms.service.impl;

import com.silaev.wms.converter.ProductDtoToProductConverter;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.exception.UploadProductException;
import com.silaev.wms.service.ProductPatcherService;
import com.silaev.wms.service.UploadProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadProductServiceImpl implements UploadProductService {

    public static final String ARTICLE_COLUMN = "article";
    public static final String NAME_COLUMN = "name";
    public static final String QUANTITY_COLUMN = "quantity";
    public static final String SIZE_COLUMN = "size";
    public static final String XLSX = "xlsx";

    private final ProductDtoToProductConverter productConverter;
    private final ProductPatcherService productPatcherService;

    @Value("${storage.bulk-upload-path}")
    private String pathToStorage;

    private String getCell(Row currentRow, int columnNumber) {
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

    public Mono<Void> patchProductQuantity(Flux<FilePart> files, final String userName) {
        return Mono.fromRunnable(() -> init(userName)).publishOn(Schedulers.newElastic("init"))
                .log(String.format("cleaning-up directory: %s", userName))
                .then(files.flatMap(f ->
                                saveFileToDiskAndUpdate(f, userName)
                                        .subscribeOn(Schedulers.elastic())
                        ).then()
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
    private Mono<Void> saveFileToDiskAndUpdate(FilePart file, String userName) {
        String fileName = file.filename();
        return Mono.just(
                Paths.get(pathToStorage, userName, fileName))
                .log(String.format("A file: %s has been uploaded", fileName))
                .flatMap(file::transferTo)
                .then(processExcelFile(fileName, userName))
                .log(String.format("A file: %s has been saved", fileName));
    }

    private Mono<Void> processExcelFile(String fileName, String userName) {
        return Mono.just(fileName)
                .flatMap(x -> {
                    File file = Paths.get(pathToStorage + "/" + userName, fileName).toFile();
                    verifyFileAttributes(file);

                    try (FileInputStream io = new FileInputStream(file);
                         Workbook workbook = new XSSFWorkbook(io)) {

                        //awareness of inserted empty rows
                        workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                        Sheet datatypeSheet = workbook.getSheetAt(0);//or iterate
                        Iterator<Row> iterator = datatypeSheet.iterator();

                        int rowCounter = 0;
                        if (iterator.hasNext()) {
                            Row currentRow = iterator.next();
                            rowCounter++;
                            verifyExcelFileHeader(fileName, currentRow);
                        }

                        Map<Product, BigInteger> productIndex = new HashMap<>();

                        while (iterator.hasNext()) {
                            Row currentRow = iterator.next();
                            rowCounter++;

                            addProductToIndex(fileName, rowCounter, productIndex, currentRow);
                        }

                        List<Long> productArticles = productIndex.keySet().stream()
                                .map(Product::getArticle)
                                .collect(Collectors.toList());

                        return productPatcherService.incrementProductQuantity(
                                fileName,
                                productIndex,
                                productArticles,
                                userName
                        );

                    } catch (IOException e) {
                        throw new UploadProductException(
                                String.format("An exception has been occurred while parsing a file: %s " +
                                        "has been saved", fileName), e);
                    }
                }).log(String.format("A file: %s has been processed", fileName))
                .then();
    }

    private void verifyFileAttributes(File file) {
        String fileName = file.getName();
        String absolutePath = file.getAbsolutePath();
        if (!FilenameUtils.isExtension(absolutePath, XLSX)) {
            throw new UploadProductException(String.format("The file: %s is supposed to be xlsx format. " +
                    "Other format are not currently supported", fileName));
        }
    }


    private void addProductToIndex(String fileName, int rowCounter, Map<Product, BigInteger> productIndex, Row currentRow) {
        Long article = Long.valueOf(getCell(currentRow, 0));
        if (Long.valueOf(0).equals(article)) {
            formBulkImportException(fileName, rowCounter, ARTICLE_COLUMN);
        }
        String name = getCell(currentRow, 1);
        if (StringUtils.isEmpty(article)) {
            formBulkImportException(fileName, rowCounter, NAME_COLUMN);
        }
        BigInteger quantity = new BigInteger(getCell(currentRow, 2));
        if (BigInteger.ZERO.equals(quantity)) {
            formBulkImportException(fileName, rowCounter, QUANTITY_COLUMN);
        }

        Integer sizeInteger = Integer.valueOf(getCell(currentRow, 3));
        if (sizeInteger == 0) {
            formBulkImportException(fileName, rowCounter, SIZE_COLUMN);
        }

        ProductDto productDto = ProductDto.builder()
                .article(article)
                .name(name)
                .quantity(quantity)
                .size(sizeInteger)
                .build();

        Product product = productConverter.convert(productDto);
        productIndex.putIfAbsent(product, BigInteger.ZERO);
        productIndex.computeIfPresent(product, (k, v) -> v.add(quantity));
    }

    private void formBulkImportException(String fileName, int rowCounter, String quantity2) {
        throw new UploadProductException(
                String.format("File: %s, an empty %s in a row: %d ", fileName, quantity2, rowCounter));
    }

    private void verifyExcelFileHeader(String fileName, Row currentRow) {
        if (!ARTICLE_COLUMN.equalsIgnoreCase(getCell(currentRow, 0))
                || (!NAME_COLUMN.equalsIgnoreCase(getCell(currentRow, 1)))
                || (!QUANTITY_COLUMN.equalsIgnoreCase(getCell(currentRow, 2)))
                || (!SIZE_COLUMN.equalsIgnoreCase(getCell(currentRow, 3)))) {

            throw new UploadProductException(String.format(
                    "File: %s has an incorrect header. Expected: %s %s %s %s",
                    fileName, ARTICLE_COLUMN, NAME_COLUMN, QUANTITY_COLUMN, SIZE_COLUMN));
        }
    }

    private void init(String userName) {
        Path rootLocation = Paths.get(pathToStorage, userName);

        //clean up the root directory
        FileSystemUtils.deleteRecursively(rootLocation.toFile());

        //init the root directory
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new UploadProductException(String.format("An error while creation the root directory for: %s", userName));
        }
    }
}
