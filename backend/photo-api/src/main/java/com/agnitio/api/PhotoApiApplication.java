package com.agnitio.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.agnitio")
@EntityScan(basePackages = "com.agnitio.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.agnitio.infrastructure.persistence")
public class PhotoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotoApiApplication.class, args);
    }
}
