package com.silaev.wms.service;

import com.silaev.wms.dto.FileUploadDto;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;


public interface UploadProductService {
  Flux<FileUploadDto> patchProductQuantity(Flux<FilePart> files, final String userName);
}
