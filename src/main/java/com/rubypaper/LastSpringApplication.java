package com.rubypaper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LastSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(LastSpringApplication.class, args);
	}

}
