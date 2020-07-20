package com.silaev.wms.entity;

import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Document(Product.COLLECTION_NAME)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"article", "size"}, callSuper = false)
@CompoundIndex(name = "article_size", def = "{'size' : 1, 'article': 1}")
@Setter(AccessLevel.NONE)
public class Product extends BaseEntity {
  public static final String COLLECTION_NAME = "product";
  public static final String ARTICLE_DB_FIELD = "article";
  public static final String QUANTITY_DB_FIELD = "quantity";
  public static final String LAST_MODIFIED_BY_DB_FIELD = "lastModifiedBy";
  public static final String SIZE_DB_FIELD = "size";

  @Indexed(unique = true)
  private Long article;

  @TextIndexed
  private String name;

  @TextIndexed
  private Brand brand;

  private BigDecimal price;

  private Size size;

  @Indexed
  private BigInteger quantity;
}
