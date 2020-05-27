package com.silaev.wms.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.math.BigInteger;

@WritingConverter
public class BigIntegerToIntegerConverter implements Converter<BigInteger, Integer> {
  @Override
  public Integer convert(BigInteger source) {
    return source.intValue();
  }
}
