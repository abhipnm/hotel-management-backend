package com.restaurantmanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4.1 defaults Jackson autoconfiguration to Jackson 3's
 * {@code tools.jackson.databind.json.JsonMapper} and no longer registers a
 * classic Jackson 2 {@link ObjectMapper} bean. Security error handlers in this
 * project still use the classic Jackson 2 API, so it's provided explicitly here.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
