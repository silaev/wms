package com.silaev.wms.converter;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.silaev.wms.entity.Product;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import reactor.util.annotation.NonNull;
import reactor.util.function.Tuple3;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Converts a product and a user name to UpdateOneModel.
 */
@Component
public class ProductAndUserNameToUpdateOneModelConverter implements
  Converter<Tuple3<Product, BigInteger, String>, UpdateOneModel<Document>> {

  @Override
  @NonNull
  public UpdateOneModel<Document> convert(@NonNull Tuple3<Product, BigInteger, String> source) {
    Objects.requireNonNull(source);
    final Product product = source.getT1();
    final BigInteger quantity = source.getT2();
    final String userName = source.getT3();

    return new UpdateOneModel<>(
      Filters.and(
        Filters.eq(Product.SIZE_DB_FIELD, product.getSize().name()),
        Filters.eq(Product.ARTICLE_DB_FIELD, product.getArticle())
      ),
      Document.parse(
        String.format(
          "{ $inc: { %s: %d } }",
          Product.QUANTITY_DB_FIELD,
          quantity
        )
      ).append(
        "$set",
        new Document(
          Product.LAST_MODIFIED_BY_DB_FIELD,
          userName
        )
      ),
      new UpdateOptions().upsert(false)
    );
  }
}
