package com.silaev.wms.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableReactiveMongoRepositories
@ConditionalOnProperty(
        value = "spring.data.mongodb.repositories.type",
        havingValue = "none"
)
public class MongoConfig {
}
