package com.silaev.wms.annotation.version;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@RequestMapping(ApiV1.BASE_URL)
public @interface ApiV1 {
    String BASE_URL = "/api/v1/product";

    @AliasFor(annotation = Component.class)
    String value() default "";
}