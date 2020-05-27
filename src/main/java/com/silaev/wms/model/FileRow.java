package com.silaev.wms.model;

import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.usermodel.Row;

/**
 * @author Konstantin Silaev on 6/20/2020
 */
@Builder
@Data
public class FileRow {
  private final String fileName;
  private final int rowCounter;
  private final Row currentRow;
}
