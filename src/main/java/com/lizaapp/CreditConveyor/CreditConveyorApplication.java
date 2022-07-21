package com.lizaapp.CreditConveyor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.lizaapp.CreditConveyor"})
public class CreditConveyorApplication {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreditConveyorApplication.class);

	public static void main(String[] args) {
		log.info("Starting application");
		SpringApplication.run(CreditConveyorApplication.class, args);
		log.info("The application has started");
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}
}
