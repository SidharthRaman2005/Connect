package com.connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Connect backend.
 *
 * Connect is a college-exclusive networking platform for VIT-AP students.
 * This is the M0.1 scaffold: web layer only, no database or security yet
 * (those arrive in M0.2 and the Phase 1 auth modules).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectApplication.class, args);
    }
}
