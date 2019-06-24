package com.silaev.wms.testutil;

import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ProductUtil {
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

    public static MultiValueMap<String, HttpEntity<?>> getMultiPartFormDataMulti() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        Resource file1 = new ClassPathResource("products1.xlsx");
        Resource file2 = new ClassPathResource("products2.xlsx");
        builder.part("file", file1).header("file", "file");
        builder.part("file", file2).header("file", "file");
        return builder.build();
    }

    public static MultiValueMap<String, HttpEntity<?>> getMultiPartFormDataSingle() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        Resource file1 = new ClassPathResource("products1.xlsx");
        builder.part("file", file1).header("file", "file");
        return builder.build();
    }

    public static String encodeQueryParam(String param) {
        return URLEncoder.encode(param, StandardCharsets.UTF_8);
    }
}
