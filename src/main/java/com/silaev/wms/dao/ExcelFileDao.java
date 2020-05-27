package com.silaev.wms.dao;

import com.silaev.wms.entity.Product;
import org.apache.poi.ss.usermodel.Row;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * Provides basic ops on an excel file.
 * <p>
 * This class is not thread safe and its instance is supposed to be
 * called withing the same thread.
 *
 * @author Konstantin Silaev on 6/20/2020
 */
public interface ExcelFileDao {
  void verifyFileAttributes(File file);

  void verifyExcelFileHeader(String fileName, Row currentRow);

  String getCell(Row currentRow, int columnNumber);

  Flux<Product> getProducts(
    final String pathToStorage,
    final String fileName,
    final String userName
  );
}
