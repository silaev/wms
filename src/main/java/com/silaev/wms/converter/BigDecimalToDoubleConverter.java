package com.silaev.wms.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.math.BigDecimal;

@WritingConverter
public class BigDecimalToDoubleConverter implements Converter<BigDecimal, Double> {
  @Override
  public Double convert(BigDecimal source) {
    return source.doubleValue();
  }
}
