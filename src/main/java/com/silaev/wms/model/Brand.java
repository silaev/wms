package com.silaev.wms.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
@Getter
public enum Brand {
  ENGLISH_LAUNDRY("ENGLISH LAUNDRY"),
  LACOSTE("LACOSTE"),
  DOLCE("DOLCE & GABBANA Dolce");

  private final String brandName;

  public static Brand byName(String brandName) {
    Objects.requireNonNull(brandName);

    for (Brand brand : values()) {
      if (brand.getBrandName().equalsIgnoreCase(brandName)) {
        return brand;
      }
    }
    throw new IllegalArgumentException(String.format(
      "Cannot find brand by %s", brandName));
  }
}
