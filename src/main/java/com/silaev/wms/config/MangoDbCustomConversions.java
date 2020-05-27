package com.silaev.wms.config;

import com.silaev.wms.converter.BigDecimalToDoubleConverter;
import com.silaev.wms.converter.BigIntegerToIntegerConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
public class MangoDbCustomConversions {

  @Bean
  public MongoCustomConversions mongoCustomConversions() {
    return new MongoCustomConversions(Arrays.asList(
      //double, Double, float, Float are native
      new BigDecimalToDoubleConverter(),

      //int, Integer, short, Short are native 32-bit integer
      //in case of extension make use of Long that is native 64-bit integer
      new BigIntegerToIntegerConverter()
    ));
  }
}
