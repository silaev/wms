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
  public Flux<Product> createProduct(Flux<ProductDto> productDto, String userName) {
    return productDto
      .map(productConverter::convert)
      .map(p -> {
        p.setCreatedBy(userName);
        return p;
      })
      .flatMap(productDao::insert);
  }

  @Override
  public Flux<Product> findAll() {
    return productDao.findAll();
  }

  @Override
  public Flux<ProductDto> findLastProducts(BigInteger lastSize) {
    return productDao.findAllByQuantityIsLessThanEqualOrderByQuantityAsc(lastSize)
      .map(productDtoConverter::convert);
  }
}
