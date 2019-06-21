package com.silaev.wms.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface UploadProductService {
    Mono<Void> patchProductQuantity(Flux<FilePart> files, final String userName);
}
