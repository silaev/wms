package com.silaev.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"article", "size"})
public class ProductDto {

    @NotNull
    private Long article;

    @NotNull
    private String name;

    @NotNull
    private String brand;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Integer size;

    @NotNull
    private BigInteger quantity;
}
