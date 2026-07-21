package com.habitiq.fileprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.habitiq")
@EnableDiscoveryClient
public class FileProcessorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileProcessorServiceApplication.class, args);
    }
}
