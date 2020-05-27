package com.silaev.wms.converter;

import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import reactor.util.annotation.NonNull;

import java.util.Optional;

@Component
public class ProductDtoToProductConverter implements Converter<ProductDto, Product> {
  @Override
  @NonNull
  public Product convert(ProductDto source) {
    return Product.builder()
      .article(source.getArticle())
      .brand(Optional.ofNullable(source.getBrand()).map(Brand::byName).orElse(null))
      .name(source.getName())
      .size(Size.bySizeInteger(source.getSize()))
      .price(source.getPrice())
      .quantity(source.getQuantity())
      .build();
  }
}
