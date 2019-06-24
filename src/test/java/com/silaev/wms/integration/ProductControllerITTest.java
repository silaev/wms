package com.silaev.wms.integration;

import com.silaev.wms.annotation.version.ApiV1;
import com.silaev.wms.converter.ProductToProductDtoConverter;
import com.silaev.wms.dao.ProductDao;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import com.silaev.wms.security.SecurityConfig;
import com.silaev.wms.testutil.ProductUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
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
import static org.junit.Assert.assertEquals;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProductControllerITTest {
    public static final String BASE_URL = ApiV1.BASE_URL;

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

    @Before
    public void setUp() {
        product1 = ProductUtil.mockProduct(120589L, "AAA", Brand.DOLCE, BigDecimal.valueOf(9), 9, Size.SIZE_50);
        product2 = ProductUtil.mockProduct(120590L, "BBB", Brand.DOLCE, BigDecimal.valueOf(15.69), 6, Size.SIZE_100);
        product3 = ProductUtil.mockProduct(120591L, "CCC", Brand.ENGLISH_LAUNDRY, BigDecimal.valueOf(55.12), 3, Size.SIZE_100);

        productDto1 = productConverter.convert(product1);
        productDto2 = productConverter.convert(product2);
        productDto3 = productConverter.convert(product3);
    }

    @After
    public void tearDown() {
        StepVerifier.create(productDao.deleteAll()).verifyComplete();
    }

    @WithMockUser(authorities = SecurityConfig.READ_PRIVILEGE)
    @Test
    public void shouldFindProductsByNameOrBrand() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2, product3));

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
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
    public void shouldFindAll() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2, product3));

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
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
    public void shouldFindLastProducts() {
        //GIVEN
        //log.debug("After insert product1:{} product2:{} product3:{}",
        // product1.getQuantity(), product2.getQuantity(), product3.getQuantity());
        insertMockProductsIntoDb(Arrays.asList(product1, product2, product3));

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
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
     * article 1647 doesn't exist
     * <p>
     * HttpStatus 500: Command failed with error 251 (NoSuchTransaction): 'Transaction 1 has been aborted.' on server localhost:27017. The full response is { "errorLabels" : ["TransientTransactionError"], "operationTime" : { "$timestamp" : { "t" : 1561124700, "i" : 2 } }, "ok" : 0.0, "errmsg" : "Transaction 1 has been aborted.", "code" : 251, "codeName" : "NoSuchTransaction", "$clusterTime" : { "clusterTime" : { "$timestamp" : { "t" : 1561124700, "i" : 2 } }, "signature" : { "hash" : { "$binary" : "AAAAAAAAAAAAAAAAAAAAAAAAAAA=", "$type" : "00" }, "keyId" : { "$numberLong" : "0" } } } }; nested exception is com.mongodb.MongoCommandException: Command failed with error 251 (NoSuchTransaction): 'Transaction 1 has been aborted.' on server localhost:27017. The full response is { "errorLabels" : ["TransientTransactionError"], "operationTime" : { "$timestamp" : { "t" : 1561124700, "i" : 2 } }, "ok" : 0.0, "errmsg" : "Transaction 1 has been aborted.", "code" : 251, "codeName" : "NoSuchTransaction", "$clusterTime" : { "clusterTime" : { "$timestamp" : { "t" : 1561124700, "i" : 2 } }, "signature" : { "hash" : { "$binary" : "AAAAAAAAAAAAAAAAAAAAAAAAAAA=", "$type" : "00" }, "keyId" : { "$numberLong" : "0" } } } }
     */

    @WithMockUser(
            username = SecurityConfig.ADMIN_NAME,
            password = SecurityConfig.ADMIN_PAS,
            authorities = SecurityConfig.WRITE_PRIVILEGE
    )
    @Test
    public void shouldNotPatchProductQuantity() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2));

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
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
                                HttpStatus.ACCEPTED.value()//TODO: occurs on Travis
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
    public void shouldPatchProductQuantity() {
        //GIVEN
        insertMockProductsIntoDb(Arrays.asList(product1, product2));
        BigInteger expected1 = BigInteger.valueOf(16);
        BigInteger expected2 = BigInteger.valueOf(27);

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient
                .patch()
                .uri(BASE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(ProductUtil.getMultiPartFormDataSingle()))
                .exchange();

        //THEN
        exchange
                .expectStatus()
                .isAccepted();

        Flux<Product> all = productDao.findAllByOrderByQuantityAsc();
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
    public void shouldCreateProducts() {
        //GIVEN
        Flux<ProductDto> dtoFlux = Flux.just(productDto1, productDto2);

        //WHEN
        WebTestClient.ResponseSpec exchange = webClient.post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
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
        Flux<Product> productFlux = Flux.fromIterable(products);
        Flux<Product> insert = productDao.deleteAll()
                .thenMany(productDao.insert(productFlux))
                .sort(Comparator.comparingLong(Product::getArticle));

        StepVerifier.create(insert)
                .expectNextSequence(products)
                .verifyComplete();
    }
}