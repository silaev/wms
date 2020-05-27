package com.silaev.wms.service;

import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import reactor.core.publisher.Flux;

import java.math.BigInteger;


public interface ProductService {
  Flux<ProductDto> findProductsByNameOrBrand(String name, Brand brand);

  Flux<Product> createProduct(Flux<ProductDto> productDto, String userName);

  Flux<Product> findAll();

  Flux<ProductDto> findLastProducts(BigInteger lastSize);
}
