package com.example.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            System.out.println("========================================");
            System.out.println("=                                      =");
            System.out.println("=   后端服务启动成功！                  =");
            System.out.println("=   Backend Service Started Successfully =");
            System.out.println("=                                      =");
            System.out.println("=   访问地址: http://localhost:8081    =");
            System.out.println("=   API 路径: /api/**                  =");
            System.out.println("=                                      =");
            System.out.println("========================================");
        };
    }
}
