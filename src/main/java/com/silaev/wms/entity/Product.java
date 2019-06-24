package com.silaev.wms.entity;

import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Document
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"article", "size"}, callSuper = false)
public class Product extends BaseEntity {

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
