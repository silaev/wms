package com.silaev.wms.dao;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateOneModel;
import org.bson.Document;
import reactor.core.publisher.Mono;

import java.util.List;


public interface ProductPatcherDao {
  Mono<BulkWriteResult> incrementProductQuantity(String fileName, List<UpdateOneModel<Document>> models, String userName);
}
