package com.silaev.wms.integration;

import com.silaev.wms.annotation.version.ApiV1;
import com.silaev.wms.converter.ProductToProductDtoConverter;
import com.silaev.wms.core.IntegrationTest;
import com.silaev.wms.dao.ProductDao;
import com.silaev.wms.dto.FileUploadDto;
import com.silaev.wms.dto.ProductDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.model.Brand;
import com.silaev.wms.model.Size;
import com.silaev.wms.security.SecurityConfig;
import com.silaev.wms.testutil.ProductTestUtil;
import com.silaev.wms.testutil.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.MongoDBContainer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.either;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "3600000")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(initializers = ProductControllerITTest.Initializer.class)
@IntegrationTest
class ProductControllerITTest {
  private static final String BASE_URL = ApiV1.BASE_URL;
  private static final MongoDBContainer MONGO_DB_CONTAINER =
    new MongoDBContainer("mongo:4.2.8");
  private final ProductToProductDtoConverter productConverter = new ProductToProductDtoConverter();
  @Autowired
  private WebTestClient webClient;
  @Autowired
  private ProductDao productDao;
  private Product product1;
  private Product product2;
  private Product product3;
  private ProductDto productDto1;
  private ProductDto productDto2;
  private ProductDto productDto3;

  @BeforeAll
  static void setUpAll() {
    MONGO_DB_CONTAINER.start();
  }

  @AfterAll
  static void tearDownAll() {
    if (!MONGO_DB_CONTAINER.isShouldBeReused()) {
      MONGO_DB_CONTAINER.stop();
    }
  }

  @BeforeEach
  void setUpBeforeEach() {
    product1 = ProductTestUtil.mockProduct(120589L, "AAA", Brand.DOLCE, BigDecimal.valueOf(9), 9, Size.SIZE_50);
    product2 = ProductTestUtil.mockProduct(120590L, "BBB", Brand.DOLCE, BigDecimal.valueOf(15.69), 6, Size.SIZE_100);
    product3 = ProductTestUtil.mockProduct(120591L, "CCC", Brand.ENGLISH_LAUNDRY, BigDecimal.valueOf(55.12), 3, Size.SIZE_100);

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
    insertMockProductsIntoDb(Flux.just(product1, product2, product3));

    //WHEN
    val exchange = webClient
      .get()
      .uri(uriBuilder -> uriBuilder.path(BASE_URL + "/all")
        .queryParam("name", "AAA")
        .queryParam("brand", ProductTestUtil.encodeQueryParam(Brand.ENGLISH_LAUNDRY.getBrandName()))
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
    insertMockProductsIntoDb(Flux.just(product1, product2, product3));

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
    insertMockProductsIntoDb(Flux.just(product1, product2, product3));

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
   * --------------------------products1.xlsx      products2.xlsx
   * -------------------------------------------------------------------------------------------------------------
   * 120589	Eau de Parfum	7	                3                    50      9                   OK/CONFLICT
   * 120590	Eau de Parfum	21	                5                    100     6                   OK/CONFLICT
   */

  @WithMockUser(
    username = SecurityConfig.ADMIN_NAME,
    password = SecurityConfig.ADMIN_PAS,
    authorities = SecurityConfig.WRITE_PRIVILEGE
  )
  @Test
  void shouldPatchProductQuantityConcurrently() {
    //GIVEN
    TransactionUtil.setMaxTransactionLockRequestTimeoutMillis(
      20,
      MONGO_DB_CONTAINER.getReplicaSetUrl()
    );
    insertMockProductsIntoDb(Flux.just(product1, product2));
    final String fileName1 = "products1.xlsx";
    final String fileName2 = "products2.xlsx";
    final String[] fileNames = {fileName1, fileName2};
    final BigInteger expected120589Sum = BigInteger.valueOf(19);
    final BigInteger expected120590Sum = BigInteger.valueOf(32);
    final BigInteger expected120589T1 = BigInteger.valueOf(16);
    final BigInteger expected120589T2 = BigInteger.valueOf(12);
    final BigInteger expected120590T1 = BigInteger.valueOf(27);
    final BigInteger expected120590T2 = BigInteger.valueOf(11);
    final FileUploadDto fileUploadDto1 = ProductTestUtil.mockFileUploadDto(fileName1, 2);
    final FileUploadDto fileUploadDto2 = ProductTestUtil.mockFileUploadDto(fileName2, 2);

    //WHEN
    final WebTestClient.ResponseSpec exchange = webClient
      .patch()
      .uri(BASE_URL)
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .accept(MediaType.APPLICATION_STREAM_JSON)
      .body(BodyInserters.fromMultipartData(ProductTestUtil.getMultiPartFormData(fileNames)))
      .exchange();

    //THEN
    exchange.expectStatus().isAccepted();
    assertThat(
      extractBodyArray(exchange),
      either(arrayContaining(fileUploadDto1))
        .or(arrayContaining(fileUploadDto2))
        .or(arrayContainingInAnyOrder(fileUploadDto1, fileUploadDto2))
    );

    final List<Product> list = productDao.findAll(Sort.by(Sort.Direction.ASC, "article"))
      .toStream().collect(Collectors.toList());
    assertThat(list.size(), is(2));

    assertThat(
      list.stream().map(Product::getQuantity).toArray(BigInteger[]::new),
      either(arrayContaining(expected120589T1, expected120590T1))
        .or(arrayContaining(expected120589T2, expected120590T2))
        .or(arrayContaining(expected120589Sum, expected120590Sum))
    );
    TransactionUtil.setMaxTransactionLockRequestTimeoutMillis(
      5,
      MONGO_DB_CONTAINER.getReplicaSetUrl()
    );
  }

  /**
   * xlsx files' content:
   * article	name	        quantity	        quantity            size    initialQuantity     expectedQuantity
   * -------------------------products1.xlsx      products3.xlsx
   * -------------------------------------------------------------------------------------------------------------
   * 120589	Eau de Parfum	7	                -                    50      9                   16
   * 120590	Eau de Parfum	21	                -                    100     6                   27
   * 120591L  Eau De Toilette -                   85                   100     3                   88
   */

  @WithMockUser(
    username = SecurityConfig.ADMIN_NAME,
    password = SecurityConfig.ADMIN_PAS,
    authorities = SecurityConfig.WRITE_PRIVILEGE
  )
  @Test
  void shouldPatchProductQuantity() {
    //GIVEN
    insertMockProductsIntoDb(Flux.just(product1, product2, product3));
    final BigInteger expected1 = BigInteger.valueOf(16);
    final BigInteger expected2 = BigInteger.valueOf(27);
    final BigInteger expected3 = BigInteger.valueOf(88);
    final String fileName1 = "products1.xlsx";
    final String fileName3 = "products3.xlsx";
    final String[] fileNames = {fileName1, fileName3};
    final FileUploadDto fileUploadDto1 = ProductTestUtil.mockFileUploadDto(fileName1, 2);
    final FileUploadDto fileUploadDto3 = ProductTestUtil.mockFileUploadDto(fileName3, 1);

    //WHEN
    final WebTestClient.ResponseSpec exchange = webClient
      .patch()
      .uri(BASE_URL)
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(BodyInserters.fromMultipartData(ProductTestUtil.getMultiPartFormData(fileNames)))
      .exchange();

    //THEN
    exchange.expectStatus().isAccepted();

    exchange.expectBodyList(FileUploadDto.class)
      .hasSize(2)
      .contains(fileUploadDto1, fileUploadDto3);

    StepVerifier.create(productDao.findAllByOrderByQuantityAsc())
      .assertNext(product -> assertEquals(expected1, product.getQuantity()))
      .assertNext(product -> assertEquals(expected2, product.getQuantity()))
      .assertNext(product -> assertEquals(expected3, product.getQuantity()))
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

  @WithMockUser(authorities = SecurityConfig.WRITE_PRIVILEGE)
  @Test
  void shouldPatchAggregatedProducts() {
    //GIVEN
    insertMockProductsIntoDb(Flux.just(product1, product2));
    final BigInteger expected1 = BigInteger.valueOf(19);
    final BigInteger expected2 = BigInteger.valueOf(36);
    final String fileName = "products_duplicates.xlsx";
    final String[] fileNames = {fileName};
    final FileUploadDto fileUploadDto1 = ProductTestUtil.mockFileUploadDto(fileName, 2);

    //WHEN
    final WebTestClient.ResponseSpec exchange = webClient
      .patch()
      .uri(BASE_URL)
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(BodyInserters.fromMultipartData(ProductTestUtil.getMultiPartFormData(fileNames)))
      .exchange();

    //THEN
    exchange.expectBodyList(FileUploadDto.class)
      .hasSize(1)
      .contains(fileUploadDto1);

    StepVerifier.create(productDao.findAllByOrderByQuantityAsc())
      .assertNext(product -> assertEquals(expected1, product.getQuantity()))
      .assertNext(product -> assertEquals(expected2, product.getQuantity()))
      .verifyComplete();
  }

  /**
   * Helper method to insert mock products into MongoDB
   *
   * @param products
   */
  private void insertMockProductsIntoDb(Flux<Product> products) {
    val insert = productDao.deleteAll()
      .thenMany(productDao.insert(products))
      .sort(Comparator.comparingLong(Product::getArticle));

    StepVerifier.create(insert)
      .expectNextSequence(products.toIterable())
      .verifyComplete();
  }

  private FileUploadDto[] extractBodyArray(final WebTestClient.ResponseSpec exchange) {
    final List<FileUploadDto> responseBody = exchange
      .expectBodyList(FileUploadDto.class)
      .returnResult()
      .getResponseBody();
    assertNotNull(responseBody);
    return responseBody.toArray(new FileUploadDto[0]);
  }

  static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
        String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl())
      ).applyTo(configurableApplicationContext);
    }
  }
}