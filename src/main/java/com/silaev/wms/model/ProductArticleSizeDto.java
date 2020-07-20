package com.silaev.wms.model;

import lombok.Value;

/**
 * @author Konstantin Silaev on 6/28/2020
 */
@Value
public class ProductArticleSizeDto {
  Long article;
  Size size;
}