package com.silaev.wms.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
@Getter
public enum Size {
  SIZE_50(50), SIZE_100(100);

  private final Integer sizeInteger;

  public static Size bySizeInteger(Integer sizeInteger) {
    Objects.requireNonNull(sizeInteger);

    for (Size size : values()) {
      if (size.getSizeInteger().equals(sizeInteger)) {
        return size;
      }
    }
    throw new IllegalArgumentException(String.format(
      "Cannot find size by %s", sizeInteger));
  }
}
