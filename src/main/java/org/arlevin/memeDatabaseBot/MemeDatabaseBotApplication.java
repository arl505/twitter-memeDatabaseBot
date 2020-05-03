package org.arlevin.memedatabasebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MemeDatabaseBotApplication {

  public static void main(String[] args) {
    SpringApplication.run(MemeDatabaseBotApplication.class, args);
  }
}