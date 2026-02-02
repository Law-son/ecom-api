package com.eyarko.ecom;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EcomApplication {

	public static void main(String[] args) {
		// Load .env file before Spring Boot starts
		try {
			Dotenv dotenv = Dotenv.configure()
				.directory(".")
				.ignoreIfMissing()
				.load();
			
			// Set each variable as a system property so Spring Boot can use them
			dotenv.entries().forEach(entry -> {
				String key = entry.getKey();
				String value = entry.getValue();
				if (System.getProperty(key) == null) {
					System.setProperty(key, value);
				}
			});
		} catch (Exception e) {
			// Silently fail if .env file doesn't exist or can't be loaded
			// This allows the app to run with environment variables set manually
		}
		
		SpringApplication.run(EcomApplication.class, args);
	}

}
