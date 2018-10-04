package com.silaev.wms.service;

import com.silaev.wms.converter.ProductDtoToProductConverter;
import com.silaev.wms.converter.ProductToProductDtoConverter;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Brand;
import com.silaev.wms.entity.Product;
import com.silaev.wms.dao.ProductDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigInteger;

@Component
@RequiredArgsConstructor
public class ProductService {
    private final ProductDao productDao;
    private final ProductDtoToProductConverter productConverter;
    private final ProductToProductDtoConverter productDtoConverter;

    public Flux<ProductDto> findProductsByNameOrBrand(String name, Brand brand) {
        return productDao.findByNameOrBrand(name, brand)
                .map(productDtoConverter::convert);
    }

    public Flux<Product> createProduct(Flux<ProductDto> productDto, String userName) {
        return productDto
                .map(productConverter::convert)
                .map(p -> {
                    p.setCreatedBy(userName);
                    return p;
                })
                .flatMap(productDao::insert);
    }

    public Flux<Product> findAll() {
        return productDao.findAll();
    }

    public Flux<ProductDto> findLastProducts(BigInteger lastSize) {
        return productDao.findAllByQuantityIsLessThanEqualOrderByQuantityAsc(lastSize)
                .map(productDtoConverter::convert);
    }
}
