package com.silaev.wms.service;

import com.silaev.wms.entity.Product;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;


public interface ProductPatcherService {
    Mono<Void> incrementProductQuantity(
            String fileName,
            Map<Product, BigInteger> productIndex,
            List<Long> productArticles, String userName
    );
}
