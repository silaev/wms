package com.silaev.wms.testutil;

import com.silaev.wms.dto.FileUploadDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ProductTestUtil {
  private ProductTestUtil() {

  }

  public static Product mockProduct(
    Long article,
    String name,
    Brand brand,
    BigDecimal price,
    Integer quantity,
    Size size50
  ) {
    return Product.builder()
      .article(article)
      .name(name)
      .brand(brand)
      .price(price)
      .quantity(BigInteger.valueOf(quantity))
      .size(size50)
      .build();
  }

  public static MultiValueMap<String, HttpEntity<?>> getMultiPartFormData(final String... files) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    for (final String file : files) {
      builder.part("file", new ClassPathResource(file)).header("file", "file");
    }
    return builder.build();
  }

  @SneakyThrows
  public static String encodeQueryParam(String param) {
    return URLEncoder.encode(param, String.valueOf(StandardCharsets.UTF_8));
  }

  public static FileUploadDto mockFileUploadDto(final String fileName, final int count) {
    return FileUploadDto.builder()
      .fileName(fileName)
      .matchedCount(count)
      .modifiedCount(count)
      .build();
  }
}
