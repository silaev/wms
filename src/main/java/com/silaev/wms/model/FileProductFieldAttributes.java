package com.silaev.wms.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Konstantin Silaev on 6/22/2020
 */
@RequiredArgsConstructor
@Getter
public enum FileProductFieldAttributes {
  ARTICLE("art"),
  NAME("description"),
  QUANTITY("qty"),
  SIZE("pack");

  private final String fileColumnName;
}
