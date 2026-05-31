package com.pascualbravo.calendario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ms1.festivos.base-url}")
    private String ms1BaseUrl;

    @Bean
    public WebClient festivosApiWebClient() {
        return WebClient.builder()
                .baseUrl(ms1BaseUrl)
                .build();
    }
}
