package com.alantek.caja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class CajaAhorrosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CajaAhorrosBackendApplication.class, args);
	}

}
