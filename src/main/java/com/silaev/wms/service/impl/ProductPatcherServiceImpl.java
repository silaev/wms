package com.silaev.wms.service.impl;

import com.silaev.wms.entity.Product;
import com.silaev.wms.exception.UploadProductException;
import com.silaev.wms.service.ProductPatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductPatcherServiceImpl implements ProductPatcherService {
    private final ReactiveMongoOperations reactiveOps;

    @Override
    public Mono<Void> incrementProductQuantity(
            String fileName,
            Map<Product, BigInteger> productIndex,
            List<Long> productArticles, String userName
    ) {
        return reactiveOps.inTransaction().execute(action ->
                action.find(query(where("article").in(productArticles)), Product.class)
                        .log("start findByArticleIn")
                        .flatMap(product -> {

                            if (product.getQuantity() == null) {
                                throw new UploadProductException(
                                        String.format("An article: %s quality that equals to null", product.getArticle()));
                            }

                            BigInteger val = productIndex.get(product);
                            if (val == null) {
                                throw new UploadProductException(
                                        String.format("Inconsistency between MongoDB and a file: " +
                                                "%s in terms of article %d and size %s and ", fileName, product.getArticle(), product.getSize()));
                            }

                            product.setQuantity(product.getQuantity().add(val));
                            product.setLastModifiedBy(userName);
                            productIndex.remove(product);

                            return action.save(product);

                        })
                        .log("end findByArticleIn")
                        .doOnComplete(() -> productIndex.keySet()
                                .forEach(k -> log.debug("The file: {}, the product with article: {} " +
                                                "and size: {} is not found in MongoDB and thus it's not updated!",
                                        fileName, k.getArticle(), k.getSize()))))
                .then();
    }
}
