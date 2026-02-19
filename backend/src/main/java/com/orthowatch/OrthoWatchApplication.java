package com.orthowatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("com.orthowatch.repository")
public class OrthoWatchApplication {

  public static void main(String[] args) {
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    SpringApplication.run(OrthoWatchApplication.class, args);
  }
}
