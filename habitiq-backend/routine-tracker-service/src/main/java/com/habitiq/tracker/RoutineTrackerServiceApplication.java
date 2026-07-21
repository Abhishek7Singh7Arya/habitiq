package com.habitiq.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.habitiq")
@EnableDiscoveryClient
@EnableScheduling
public class RoutineTrackerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoutineTrackerServiceApplication.class, args);
    }
}
