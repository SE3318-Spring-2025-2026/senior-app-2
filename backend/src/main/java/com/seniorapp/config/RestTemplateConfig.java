package com.seniorapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(org.springframework.boot.web.client.RestTemplateBuilder builder) {
        // Ensure outbound calls (e.g., LLM validation) respect connect/read time budgets.
        return builder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(25))
                .build();
    }

}
