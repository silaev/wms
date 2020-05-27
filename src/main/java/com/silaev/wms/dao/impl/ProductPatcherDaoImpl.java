package com.silaev.wms.dao.impl;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.silaev.wms.dao.ProductPatcherDao;
import com.silaev.wms.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Bulk updates products withing a MongoDB native transaction.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ProductPatcherDaoImpl implements ProductPatcherDao {
  private final TransactionalOperator transactionalOperator;
  private final ReactiveMongoOperations reactiveMongoOperations;

  @Override
  public Mono<BulkWriteResult> incrementProductQuantity(
    final String fileName,
    final List<UpdateOneModel<Document>> models,
    final String userName
  ) {
    return transactionalOperator.execute(
      action -> reactiveMongoOperations.getCollection(Product.COLLECTION_NAME)
        .flatMap(collection ->
          Mono.from(collection.bulkWrite(models, new BulkWriteOptions().ordered(true)))

        ).<BulkWriteResult>handle((bulkWriteResult, synchronousSink) -> {
          final int fileCount = models.size();
          if (Objects.equals(bulkWriteResult.getModifiedCount(), fileCount)) {
            synchronousSink.next(bulkWriteResult);
          } else {
            synchronousSink.error(
              new IllegalStateException(
                String.format(
                  "Inconsistency between modified doc count: %d and file doc count: %d. Please, check file: %s",
                  bulkWriteResult.getModifiedCount(), fileCount, fileName
                )
              )
            );
          }

        }).onErrorResume(
          e -> Mono.fromRunnable(action::setRollbackOnly)
            .log("Exception while incrementProductQuantity: " + fileName + ": " + e)
            .then(Mono.empty())
        )
    ).singleOrEmpty();
  }
}