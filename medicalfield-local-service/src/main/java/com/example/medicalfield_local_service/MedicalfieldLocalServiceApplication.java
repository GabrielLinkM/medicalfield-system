package com.example.medicalfield_local_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MedicalfieldLocalServiceApplication {
	@org.springframework.beans.factory.annotation.Value("${medicalfield.api.key:medicalfield-secret-key-2026}")
    private String apiKey;

    @Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-API-Key", apiKey);
            return execution.execute(request, body);
        });
        return restTemplate;
    }

	public static void main(String[] args) {
		SpringApplication.run(MedicalfieldLocalServiceApplication.class, args);
	}

}
