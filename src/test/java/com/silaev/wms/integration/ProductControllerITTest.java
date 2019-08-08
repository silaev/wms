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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.isIn;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Add 127.0.0.1 mongo1 mongo2 mongo3 host.docker.internal to the OS host
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(initializers = ProductControllerITTest.Initializer.class)
class ProductControllerITTest {
    private static final String BASE_URL = ApiV1.BASE_URL;

    private final static Network network = Network.newNetwork();
    private final static GenericContainer mongo2 = new GenericContainer<>("s256/wms-mongo:4.0.10")
            .withNetwork(network)
            .withNetworkAliases("mongo2")
            .withExposedPorts(27017)
            .withCommand("--replSet", "docker-rs", "--bind_ip", "localhost,mongo2")
            .waitingFor(
                    Wait.forLogMessage(".*waiting for connections on port.*", 1)
            );
    private final static GenericContainer mongo3 = new GenericContainer<>("s256/wms-mongo:4.0.10")
            .withNetwork(network)
            .withNetworkAliases("mongo3")
            .withExposedPorts(27017)
            .withCommand("--replSet", "docker-rs", "--bind_ip", "localhost,mongo3")
            .waitingFor(
                    Wait.forLogMessage(".*waiting for connections on port.*", 1)
            );
    private final static GenericContainer mongo1 = new GenericContainer<>("s256/wms-mongo:4.0.10")
            .withNetwork(network)
            .dependsOn(Arrays.asList(mongo2, mongo3))
            .withNetworkAliases("mongo1")
            .withExposedPorts(27017)
            .withCommand("--replSet", "docker-rs", "--bind_ip", "localhost,mongo1")
            .waitingFor(
                    Wait.forLogMessage(".*waiting for connections on port.*", 1)
            );
    private static final int AWAIT_MASTER_NODE_ATTEMPTS = 29;
    private static String MONGO_URL_1;
    private static String MONGO_URL_2;
    private static String MONGO_URL_3;

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

    @BeforeAll
    static void setUpBeforeAll() throws IOException, InterruptedException {
        mongo2.start();
        mongo3.start();
        mongo1.start();

        MONGO_URL_1 = "host.docker.internal:" + mongo1.getMappedPort(27017);
        MONGO_URL_2 = "host.docker.internal:" + mongo2.getMappedPort(27017);
        MONGO_URL_3 = "host.docker.internal:" + mongo3.getMappedPort(27017);

        log.debug(
                mongo1.execInContainer(
                        "mongo", "--eval", getMongoRsInitString()
                ).getStdout()
        );

        log.debug("Awaiting mongo1 to be a master node for {} attempts", AWAIT_MASTER_NODE_ATTEMPTS);
        Container.ExecResult execResultWaitForMaster = mongo1.execInContainer(
                "mongo", "--eval",
                String.format(
                        "var attempt = 0; " +
                                "while" +
                                "(db.runCommand( { isMaster: 1 } ).ismaster==false) " +
                                "{ " +
                                "if (attempt > %d) {quit(1);} " +
                                "print('awaiting mongo1 to be a master node ' + attempt); sleep(1000);  attempt++; " +
                                " }", AWAIT_MASTER_NODE_ATTEMPTS
                )
        );
        log.debug(
                execResultWaitForMaster.getStdout()
        );

        if (execResultWaitForMaster.getExitCode() == 1) {
            String errorMessage = String.format(
                    "The master node was not initialized in a set timeout: %d attempts",
                    AWAIT_MASTER_NODE_ATTEMPTS
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.debug(
                mongo1.execInContainer("mongo", "--eval", "rs.status()").getStdout()
        );
    }

    @NotNull
    private static String getMongoRsInitString() {
        return String.format(
                "rs.initiate({\n" +
                        "    \"_id\": \"docker-rs\",\n" +
                        "    \"members\": [\n" +
                        "        {\"_id\": 0, \"host\": \"%s\"},\n" +
                        "        {\"_id\": 1, \"host\": \"%s\"},\n" +
                        "        {\"_id\": 2, \"host\": \"%s\"}\n" +
                        "    ]\n" +
                        "});",
                MONGO_URL_1, MONGO_URL_2, MONGO_URL_3
        );
    }

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
    void shouldFindAll() {
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
    void shouldFindLastProducts() {
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
    void shouldNotPatchProductQuantity() {
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
    void shouldCreateProducts() {
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

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            String mongoUrl = String.format(
                    "spring.data.mongodb.uri: mongodb://%s,%s,%s/test?replicaSet=docker-rs",
                    MONGO_URL_1, MONGO_URL_2, MONGO_URL_3
            );
            TestPropertyValues values = TestPropertyValues.of(mongoUrl);
            values.applyTo(configurableApplicationContext);
            log.debug("mongoUrl: {}", mongoUrl);
        }
    }
}