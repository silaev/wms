package com.silaev.wms.integration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silaev.wms.annotation.version.ApiV1;
import com.silaev.wms.core.IntegrationTest;
import com.silaev.wms.dto.FileUploadDto;
import com.silaev.wms.entity.Product;
import com.silaev.wms.security.SecurityConfig;
import com.silaev.wms.testutil.ProductTestUtil;
import com.silaev.wms.testutil.TransactionUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.MongoDBContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "3600000")
@ActiveProfiles("test-big-file")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(initializers = PatchProductLoadITTest.Initializer.class)
@IntegrationTest
class PatchProductLoadITTest {
  private static final String BASE_URL = ApiV1.BASE_URL;
  private static final MongoDBContainer MONGO_DB_CONTAINER =
    new MongoDBContainer("mongo:4.2.8");
  @Autowired
  private WebTestClient webClient;
  @Autowired
  private ReactiveMongoTemplate reactiveMongoTemplate;

  @BeforeAll
  static void setUpAll() {
    MONGO_DB_CONTAINER.start();
  }

  @AfterAll
  static void tearDownAll() {
    MONGO_DB_CONTAINER.stop();
  }

  @WithMockUser(
    username = SecurityConfig.ADMIN_NAME,
    password = SecurityConfig.ADMIN_PAS,
    authorities = SecurityConfig.WRITE_PRIVILEGE
  )
  @Test
  void shouldPatchProductQuantityBigFile() {
    //GIVEN
    unzipClassPathFile("products_1M.zip");

    final String fileName = "products_1M.xlsx";
    final int count = 1000000;
    final long totalQuantity = 500472368779L;
    final List<Document> products = getDocuments(count);

    TransactionUtil.setTransactionLifetimeLimitSeconds(900, MONGO_DB_CONTAINER.getReplicaSetUrl());

    StepVerifier.create(
      reactiveMongoTemplate.remove(new Query(), Product.COLLECTION_NAME)
        .then(reactiveMongoTemplate.getCollection(Product.COLLECTION_NAME))
        .flatMapMany(c -> c.insertMany(products))
        .switchIfEmpty(Mono.error(new RuntimeException("Cannot insertMany")))
        .then(getTotalQuantity())
    ).assertNext(t -> assertEquals(totalQuantity, t)).verifyComplete();

    //WHEN
    final Instant start = Instant.now();
    final WebTestClient.ResponseSpec exchange = webClient
      .patch()
      .uri(BASE_URL)
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .accept(MediaType.APPLICATION_STREAM_JSON)
      .body(BodyInserters.fromMultipartData(ProductTestUtil.getMultiPartFormData("products_1M.xlsx")))
      .exchange();

    //THEN
    exchange
      .expectStatus()
      .isAccepted()
      .expectBodyList(FileUploadDto.class)
      .contains(ProductTestUtil.mockFileUploadDto(fileName, count));
    StepVerifier.create(getTotalQuantity())
      .assertNext(t -> assertEquals(totalQuantity * 2, t))
      .verifyComplete();
    log.debug("============= shouldPatchProductQuantityBigFile elapsed {}s =============", Duration.between(start, Instant.now()).toMinutes());
  }

  @SneakyThrows
  private List<Document> getDocuments(int count) {
    final ObjectMapper mapper = new ObjectMapper();
    final ArrayList<Document> documents = new ArrayList<>(count);
    try (
      final InputStream is = new FileInputStream("files-test/products_1M.json")
    ) {
      try (JsonParser jsonParser = mapper.getFactory().createParser(is)) {
        if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
          throw new IllegalStateException("Expected content to be an array");
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
          final Document document = mapper.readValue(jsonParser, Document.class);
          document.remove("_id");
          documents.add(document);
        }
      }
    }
    return documents;
  }

  @NotNull
  private Mono<Long> getTotalQuantity() {
    return Mono.from(
      reactiveMongoTemplate.aggregate(Aggregation.newAggregation(
        Aggregation.group().sum(Product.QUANTITY_DB_FIELD).as("total"),
        Aggregation.project("total").andExclude("_id")
      ), Product.class, Document.class)
        .map(x -> x.getLong("total"))
    );
  }

  private void unzipClassPathFile(String zipFileName) {
    final ClassPathResource classPathResource = new ClassPathResource(zipFileName);
    final byte[] buffer = new byte[8192];
    final Path outDir = Paths.get("files-test").normalize().toAbsolutePath();
    FileSystemUtils.deleteRecursively(outDir.toFile());
    try {
      Files.createDirectories(outDir);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    try (
      final FileInputStream fis = new FileInputStream(classPathResource.getFile());
      final BufferedInputStream bis = new BufferedInputStream(fis);
      final ZipInputStream stream = new ZipInputStream(bis)
    ) {
      ZipEntry entry;
      while ((entry = stream.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          throw new IllegalArgumentException(String.format("%s contains directories", zipFileName));
        }
        final Path filePath = outDir.resolve(entry.getName());
        final File file = filePath.toFile();
        try (
          final FileOutputStream fos = new FileOutputStream(file);
          final BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)
        ) {
          int len;
          while ((len = stream.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
          }
        } catch (IOException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
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