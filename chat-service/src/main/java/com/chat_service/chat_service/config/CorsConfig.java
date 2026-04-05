package com.chat_service.chat_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    // CORS is handled by API Gateway
    // This empty configuration disables Spring Boot's default CORS handling
    // to prevent duplicate CORS headers
}