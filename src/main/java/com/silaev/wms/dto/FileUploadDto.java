package com.silaev.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Konstantin Silaev on 6/17/2020
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadDto {
  private String fileName;
  private int matchedCount;
  private int modifiedCount;
}
