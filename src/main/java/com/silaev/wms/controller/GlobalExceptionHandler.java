package com.silaev.wms.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UncategorizedMongoDbException.class)
    public ResponseEntity<String> conflict(UncategorizedMongoDbException rte) {
        log.debug("GlobalExceptionHandler: conflict. {}", rte.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(rte.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        log.debug(
                "GlobalExceptionHandler: handleGeneralException. message: {}, cause: {}",
                e.getMessage(), e.getCause()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }
}
