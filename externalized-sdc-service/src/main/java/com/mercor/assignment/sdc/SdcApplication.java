package com.mercor.assignment.sdc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;

@SpringBootApplication(exclude = {
    BatchAutoConfiguration.class
})
public class SdcApplication {

  public static void main(String[] args) {
    SpringApplication.run(SdcApplication.class, args);
  }

}
