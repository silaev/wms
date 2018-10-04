package com.silaev.wms.converter;

import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Brand;
import com.silaev.wms.entity.Product;
import com.silaev.wms.entity.Size;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductToProductDtoConverter implements Converter<Product, ProductDto> {
    @Override
    public ProductDto convert(Product source) {
        return ProductDto.builder()
                .article(source.getArticle())
                .brand(Optional.ofNullable(source.getBrand()).map(Brand::getBrandName).orElse(null))
                .name(source.getName())
                .size(Optional.ofNullable(source.getSize()).map(Size::getSizeInteger).orElse(null))
                .price(source.getPrice())
                .quantity(source.getQuantity())
                .build();
    }
}
