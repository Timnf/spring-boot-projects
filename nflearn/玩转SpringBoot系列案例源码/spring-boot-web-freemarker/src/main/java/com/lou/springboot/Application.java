package com.lou.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello shiyanlou!
 *
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        System.out.println("启动 Spring Boot...");
        System.out.println("Demo project for Spring Boot 整合 Freemarker");
        SpringApplication.run(Application.class, args);
    }
}