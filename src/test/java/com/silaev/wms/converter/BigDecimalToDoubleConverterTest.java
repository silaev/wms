package com.silaev.wms.converter;


import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class BigDecimalToDoubleConverterTest {
    private final BigDecimalToDoubleConverter converter =
            new BigDecimalToDoubleConverter();

    @Test
    public void shouldConvertBigDecimalToDoubleTest() {
        //GIVEN
        BigDecimal bigDecimalValue = BigDecimal.TEN;
        Double doubleValueExpected = Double.valueOf("10");

        //WHEN
        Double doubleValue = converter.convert(bigDecimalValue);

        //THEN
        assertEquals(doubleValueExpected, doubleValue);
    }
}