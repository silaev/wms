package com.silaev.wms.integration;

import com.silaev.mongoextension.MongoReplicaSetExtension;
import com.silaev.wms.annotation.version.ApiV1;
import com.silaev.wms.converter.ProductToProductDtoConverter;
import com.silaev.wms.core.IntegrationTest;
import com.silaev.wms.dao.ProductDao;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import com.silaev.wms.security.SecurityConfig;
import com.silaev.wms.testutil.ProductUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.isIn;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(initializers = ProductControllerITTest.Initializer.class)
@IntegrationTest
class ProductControllerITTest {
    private static final String BASE_URL = ApiV1.BASE_URL;
    @RegisterExtension
    static MongoReplicaSetExtension MONGO_REPLICA_SET = MongoReplicaSetExtension.builder()
            .replicaSetNumber(3)
            .mongoDockerImageName("mongo:4.2.0")
            .build();

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private ProductToProductDtoConverter productConverter;
    @Autowired
    private ProductDao productDao;
    private Product product1;
    private Product product2;
    private Product product3;
    private ProductDto productDto1;
    private ProductDto productDto2;
    private ProductDto productDto3;

    @BeforeEach
    void setUpBeforeEach() {
        product1 = ProductUtil.mockProduct(120589L, "AAA", Brand.DOLCE, BigDecimal.valueOf(9), 9, Size.SIZE_50);
        product2 = ProductUtil.mockProduct(120590L, "BBB", Brand.DOLCE, BigDecimal.valueOf(15.69), 6, Size.SIZE_100);
        product3 = ProductUtil.mockProduct(120591L, "CCC", Brand.ENGLISH_LAUNDRY, BigDecimal.valueOf(55.12), 3, Size.SIZE_100);

        productDto1 = productConverter.convert(product1);
        productDto2 = productConverter.convert(product2);
        productDto3 = productConverter.convert(product3);
    }

    @AfterEach
    void tearDown() {
        StepVerifier.create(productDao.deleteAll()).verifyComplete();
    }

    @WithMockUser(authorities = SecurityConfig.READ_PRIVILEGE)
    @Test
    void shouldFindProductsByNameOrBrand() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2, product3));

        //WHEN
        val exchange = webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path(BASE_URL + "/all")
                        .queryParam("name", "AAA")
                        .queryParam("brand", ProductUtil.encodeQueryParam(Brand.ENGLISH_LAUNDRY.getBrandName()))
                        .build())
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .exchange();
        //THEN
        exchange
                .expectStatus()
                .isOk()
                .expectBodyList(ProductDto.class)
                .contains(productDto1, productDto3)//without order
                .hasSize(2);
    }

    @WithMockUser(authorities = SecurityConfig.READ_PRIVILEGE)
    @Test
    void shouldFindAll() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2, product3));

        //WHEN
        val exchange = webClient
                .get()
                .uri(BASE_URL + "/admin/all")
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .exchange();
        //THEN
        exchange
                .expectStatus()
                .isOk()
                .expectBodyList(Product.class)
                .contains(product1, product2, product3)
                .hasSize(3);
    }

    @WithMockUser(authorities = SecurityConfig.READ_PRIVILEGE)
    @Test
    void shouldFindLastProducts() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2, product3));

        //WHEN
        val exchange = webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path(BASE_URL + "/last")
                        //.queryParam("lastSize", "5") by default
                        .build())
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .exchange();
        //THEN
        exchange
                .expectStatus()
                .isOk()
                .expectBodyList(ProductDto.class)
                .contains(productDto3)
                .hasSize(1);
    }

    /**
     * xlsx files' content:
     * article	name	        quantity	        quantity            size    initialQuantity     expectedQuantity
     * products1.xlsx      products2.xlsx
     * -------------------------------------------------------------------------------------------------------------
     * 120589	Eau de Parfum	7	                3                    50      9                   7+9+3=19
     * 120590	Eau de Parfum	21	                5                    100     6                   21+6+5=32
     * 1647	    Eau de Parfum	79	                -                    50      -                   NON*
     * <p>
     * article 1647 doesn't exist
     * <p>
     * ACCEPTED: single thread
     * CONFLICT, INTERNAL_SERVER_ERROR: multithreaded
     */

    @WithMockUser(
            username = SecurityConfig.ADMIN_NAME,
            password = SecurityConfig.ADMIN_PAS,
            authorities = SecurityConfig.WRITE_PRIVILEGE
    )
    @Test
    void shouldTestConcurrentPatchProductQuantity() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2));

        //WHEN
        val exchange = webClient
                .patch()
                .uri(BASE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(ProductUtil.getMultiPartFormDataMulti()))
                .exchange();

        //THEN
        exchange
                .expectStatus().value(
                isIn(
                        Arrays.asList(
                                HttpStatus.CONFLICT.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.ACCEPTED.value()
                        )
                )
        );
    }

    /**
     * xlsx files' content:
     * article	name	        quantity	        quantity            size    initialQuantity     expectedQuantity
     * products1.xlsx      products2.xlsx
     * -------------------------------------------------------------------------------------------------------------
     * 120589	Eau de Parfum	7	                3                    50      9                   7+9+3=19
     * 120590	Eau de Parfum	21	                5                    100     6                   21+6+5=32
     * 1647	    Eau de Parfum	79	                -                    50      -                   NON*
     * article 1647 doesn't exist
     */

    @WithMockUser(
            username = SecurityConfig.ADMIN_NAME,
            password = SecurityConfig.ADMIN_PAS,
            authorities = SecurityConfig.WRITE_PRIVILEGE
    )
    @Test
    void shouldPatchProductQuantity() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2));
        BigInteger expected1 = BigInteger.valueOf(16);
        BigInteger expected2 = BigInteger.valueOf(27);

        //WHEN
        val exchange = webClient
                .patch()
                .uri(BASE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(ProductUtil.getMultiPartFormDataSingle()))
                .exchange();

        //THEN
        exchange
                .expectStatus()
                .isAccepted();

        val all = productDao.findAllByOrderByQuantityAsc();
        StepVerifier.create(all)
                .assertNext(x -> {
                    assertEquals(expected1, x.getQuantity());
                })
                .assertNext(x -> {
                    assertEquals(expected2, x.getQuantity());
                })
                .verifyComplete();
    }

    @WithMockUser(authorities = SecurityConfig.WRITE_PRIVILEGE)
    @Test
    void shouldCreateProducts() {
        //GIVEN
        val dtoFlux = Flux.just(productDto1, productDto2);

        //WHEN
        val exchange = webClient.post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .body(dtoFlux, ProductDto.class)
                .exchange();

        //THEN
        exchange.expectStatus()
                .isCreated()
                .expectBodyList(Product.class)
                .contains(product1, product2)
                .hasSize(2);
    }

    /**
     * Helper method to insert mock products into MongoDB
     *
     * @param products
     */
    private void insertMockProductsIntoDb(List<Product> products) {
        val productFlux = Flux.fromIterable(products);
        val insert = productDao.deleteAll()
                .thenMany(productDao.insert(productFlux))
                .sort(Comparator.comparingLong(Product::getArticle));

        StepVerifier.create(insert)
                .expectNextSequence(products)
                .verifyComplete();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            if (MONGO_REPLICA_SET.isEnabled()) {
                TestPropertyValues.of(
                        "spring.data.mongodb.uri: " + MONGO_REPLICA_SET.getMongoRsUrl()
                ).applyTo(configurableApplicationContext);
            }
        }
    }
}