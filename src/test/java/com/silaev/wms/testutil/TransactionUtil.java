package com.silaev.wms.testutil;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.Document;
import reactor.test.StepVerifier;

/**
 * @author Konstantin Silaev on 3/19/2020
 */
public class TransactionUtil {
  private TransactionUtil() {
  }

  public static void setTransactionLifetimeLimitSeconds(
    final int duration,
    final String replicaSetUrl
  ) {
    setMongoParameter("transactionLifetimeLimitSeconds", duration, replicaSetUrl);
  }

  public static void setMaxTransactionLockRequestTimeoutMillis(
    final int duration,
    final String replicaSetUrl
  ) {
    setMongoParameter("maxTransactionLockRequestTimeoutMillis", duration, replicaSetUrl);
  }

  private static void setMongoParameter(
    final String param,
    int duration,
    String replicaSetUrl
  ) {
    try (final MongoClient mongoReactiveClient = MongoClients.create(
      ConnectionUtil.getMongoClientSettingsWithTimeout(replicaSetUrl)
    )) {

      StepVerifier.create(mongoReactiveClient.getDatabase("admin").runCommand(
        new Document("setParameter", 1).append(param, duration)
      )).expectNextCount(1)
        .verifyComplete();
    }
  }
}
