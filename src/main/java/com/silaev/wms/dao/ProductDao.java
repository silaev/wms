package com.silaev.wms.dao;

import com.silaev.wms.entity.Brand;
import com.silaev.wms.entity.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface ProductDao extends ReactiveMongoRepository<Product, String> {
    Flux<Product> findByNameOrBrand(String name, Brand brand);

    Flux<Product> findByArticleIn(List<Long> articles);

    Flux<Product> findAll();

    Flux<Product> findAllByQuantityIsLessThanEqualOrderByQuantityAsc(BigInteger lastSize);
}