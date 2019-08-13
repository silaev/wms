package com.silaev.wms.integration;

import com.silaev.wms.annotation.version.ApiV1;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.security.SecurityConfig;
import com.silaev.wms.service.ProductService;
import com.silaev.wms.service.UploadProductService;
import com.silaev.wms.testutil.ProductUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

@WebFluxTest(
        properties = "spring.data.mongodb.repositories.type=none"
)
@Import(value = SecurityConfig.class)
@ActiveProfiles("test")
class SecurityITTest {
    private static final String BASE_URL = ApiV1.BASE_URL;

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private ProductService productService;

    @MockBean
    private UploadProductService uploadProductService;

    @Test
    void shouldNotExecuteGetBecauseIsUnauthorized() {
        //GIVEN

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
                .get()
                .uri(BASE_URL + "/all")
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .exchange();
        //THEN
        exchange
                .expectStatus()
                .isUnauthorized();
    }

    @WithMockUser(
            username = SecurityConfig.ADMIN_NAME,
            password = SecurityConfig.ADMIN_PAS + "*"
    )
    @Test
    void shouldNotExecuteGetBecauseIsForbidden() {
        //GIVEN

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
                .get()
                .uri(BASE_URL + "/all")
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .exchange();
        //THEN
        exchange
                .expectStatus()
                .isForbidden();
    }

    @WithMockUser(
            authorities = SecurityConfig.READ_PRIVILEGE
    )
    @Test
    void shouldNotExecutePostBecauseIsForbidden() {
        //GIVEN

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient.post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .body(Flux.empty(), ProductDto.class)
                .exchange();

        //THEN
        exchange.expectStatus()
                .isForbidden();
    }

    @WithMockUser(
            authorities = SecurityConfig.READ_PRIVILEGE
    )
    @Test
    void shouldNotExecutePatchBecauseIsForbidden() {
        //GIVEN

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
                .patch()
                .uri(BASE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(ProductUtil.getMultiPartFormDataMulti()))
                .exchange();

        //THEN
        exchange
                .expectStatus()
                .isForbidden();
    }
}
