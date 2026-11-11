package com.agnitio.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.agnitio")
@EntityScan(basePackages = "com.agnitio.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.agnitio.infrastructure.persistence")
public class ImageWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageWorkerApplication.class, args);
    }
}
