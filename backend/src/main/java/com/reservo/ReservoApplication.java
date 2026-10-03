package com.reservo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.reservo.mongo.MongoRepos;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
// The Mongo repositories are nested interfaces inside MongoRepos, which Spring Data skips unless told otherwise.
@EnableMongoRepositories(basePackageClasses = MongoRepos.class, considerNestedRepositories = true)
public class ReservoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservoApplication.class, args);
    }
}
