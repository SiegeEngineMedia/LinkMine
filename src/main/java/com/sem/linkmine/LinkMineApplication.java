package com.sem.linkmine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ServletComponentScan
public class LinkMineApplication {
    public static void main(String[] args) {
        SpringApplication.run(LinkMineApplication.class, args);
    }
}
