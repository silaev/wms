package com.silaev.wms.util;

import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Brand;
import com.silaev.wms.entity.Product;
import com.silaev.wms.entity.Size;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import java.math.BigDecimal;
import java.math.BigInteger;

public class ProductUtil {
    public static ProductDto mockProductDto(Long article,
                                             String name,
                                             Brand brand,
                                             BigDecimal price,
                                             Integer quantity,
                                             Size size50) {
        return ProductDto.builder()
                .article(article)
                .name(name)
                .brand(brand.getBrandName())
                .price(price)
                .quantity(BigInteger.valueOf(quantity))
                .size(size50.getSizeInteger())
                .build();
    }

    public static Product mockProduct(Long article,
                                            String name,
                                            Brand brand,
                                            BigDecimal price,
                                            Integer quantity,
                                            Size size50) {
        return Product.builder()
                .article(article)
                .name(name)
                .brand(brand)
                .price(price)
                .quantity(BigInteger.valueOf(quantity))
                .size(size50)
                .build();
    }

    public static MultiValueMap<String, HttpEntity<?>> getMultiPartFormData() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        Resource file = new ClassPathResource("products.xlsx");
        builder.part("file", file).header("file", "file");
        return builder.build();
    }
}
