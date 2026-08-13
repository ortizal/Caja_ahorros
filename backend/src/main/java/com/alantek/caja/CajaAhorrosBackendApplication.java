package com.alantek.caja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CajaAhorrosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CajaAhorrosBackendApplication.class, args);
	}

}
