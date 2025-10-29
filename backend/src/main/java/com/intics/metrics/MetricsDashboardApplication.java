package com.intics.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MetricsDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetricsDashboardApplication.class, args);
    }
}
