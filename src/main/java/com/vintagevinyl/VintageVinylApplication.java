package com.vintagevinyl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.vintagevinyl.repository")
public class VintageVinylApplication {
    public static void main(String[] args) {
        SpringApplication.run(VintageVinylApplication.class, args);
    }
}