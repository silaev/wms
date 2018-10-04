package com.silaev.wms.converter;

import org.springframework.core.convert.converter.Converter;

import java.math.BigInteger;

public class BigIntegerToIntegerConverter implements Converter<BigInteger, Integer> {
    @Override
    public Integer convert(BigInteger source) {
        return source.intValue();
    }
}
