package com.example.gitprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the GIT File Processor Spring Boot application.
 *
 * Run:
 *   java -jar git-file-processor.jar
 *   java -jar git-file-processor.jar --server.port=9090
 */
@SpringBootApplication
public class GitFileProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitFileProcessorApplication.class, args);
    }
}
