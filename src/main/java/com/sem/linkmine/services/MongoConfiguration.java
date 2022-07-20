package com.sem.linkmine.services;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfiguration {
    private ConstantsService consts;

    public MongoConfiguration(ConstantsService consts) {
        this.consts = consts;
    }

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(consts.MONGO_URI);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), consts.MONGO_DB_NAME);
    }
}
