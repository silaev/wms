package com.silaev.wms.config;

import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableMongoAuditing
@EnableTransactionManagement
@ConditionalOnRepositoryType(store = "mongodb", type = RepositoryType.REACTIVE)
public class MongoConfig {
  @Bean
  ReactiveTransactionManager reactiveTransactionManager(ReactiveMongoDatabaseFactory cf) {
    return new ReactiveMongoTransactionManager(cf);
  }

  @Bean
  TransactionalOperator transactionalOperator(ReactiveTransactionManager txm) {
    return TransactionalOperator.create(txm);
  }
}
