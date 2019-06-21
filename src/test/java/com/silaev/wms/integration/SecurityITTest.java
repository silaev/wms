package com.silaev.wms.integration;

import com.silaev.wms.annotation.version.ApiV1;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.security.SecurityConfig;
import com.silaev.wms.testutil.ProductUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class SecurityITTest {
    public static final String BASE_URL = ApiV1.BASE_URL;

    @Autowired
    private WebTestClient webClient;

    @Test
    public void shouldNotExecuteGetBecauseIsUnauthorized() {
        //GIVEN

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
                .get()
                .uri(BASE_URL + "/all")
                .accept(MediaType.APPLICATION_JSON)
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
    public void shouldNotExecuteGetBecauseIsForbidden() {
        //GIVEN

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
                .get()
                .uri(BASE_URL + "/all")
                .accept(MediaType.APPLICATION_JSON)
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
    public void shouldNotExecutePostBecauseIsForbidden() {
        //GIVEN

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient.post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
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
    public void shouldNotExecutePatchBecauseIsForbidden() {
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
