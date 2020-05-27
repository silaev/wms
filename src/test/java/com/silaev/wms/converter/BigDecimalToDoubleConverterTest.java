package com.silaev.wms.converter;


import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigDecimalToDoubleConverterTest {
  private final BigDecimalToDoubleConverter converter =
    new BigDecimalToDoubleConverter();

  @Test
  void shouldConvertBigDecimalToDoubleTest() {
    //GIVEN
    BigDecimal bigDecimalValue = BigDecimal.TEN;
    Double doubleValueExpected = Double.valueOf("10");

    //WHEN
    Double doubleValue = converter.convert(bigDecimalValue);

    //THEN
    assertEquals(doubleValueExpected, doubleValue);
  }
}