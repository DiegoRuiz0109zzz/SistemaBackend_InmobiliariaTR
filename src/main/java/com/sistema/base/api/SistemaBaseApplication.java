package com.sistema.base.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaBaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaBaseApplication.class, args);
	}

}
