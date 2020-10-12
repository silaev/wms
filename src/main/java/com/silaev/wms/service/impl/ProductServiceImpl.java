package com.silaev.wms.service.impl;

import com.silaev.wms.converter.ProductDtoToProductConverter;
import com.silaev.wms.converter.ProductToProductDtoConverter;
import com.silaev.wms.dao.ProductDao;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import com.silaev.wms.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigInteger;

@Component
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
  private final ProductDao productDao;
  private final ProductDtoToProductConverter productConverter;
  private final ProductToProductDtoConverter productDtoConverter;

  @Override
  public Flux<ProductDto> findProductsByNameOrBrand(String name, Brand brand) {
    return productDao.findByNameOrBrand(name, brand)
      .map(productDtoConverter::convert);
  }

  @Override
  public Flux<ProductDto> createProduct(Flux<ProductDto> productDto, String userName) {
    return productDto
      .map(dto -> toProduct(dto, userName))
      .flatMap(productDao::insert)
      .map(productDtoConverter::convert);

  }

  private Product toProduct(ProductDto productDto, String userName) {
    final Product product = productConverter.convert(productDto);
    product.setCreatedBy(userName);
    return product;
  }

  @Override
  public Flux<ProductDto> findAll() {
    return productDao.findAll().map(productDtoConverter::convert);
  }

  @Override
  public Flux<ProductDto> findLastProducts(BigInteger lastSize) {
    return productDao.findAllByQuantityIsLessThanEqualOrderByQuantityAsc(lastSize)
      .map(productDtoConverter::convert);
  }
}
