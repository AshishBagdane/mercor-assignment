package com.mercor.assignment.scd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mercor.assignment.scd")
public class MercorScdApplication {

	public static void main(String[] args) {
		SpringApplication.run(MercorScdApplication.class, args);
	}

}
