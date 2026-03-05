package com.hotel.api_gateway.Config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r.path("/user-service/**")
                        .filters(f -> f.rewritePath("/user-service/(?<segment>.*)", "/${segment}"))
                        .uri("lb://user-service"))
                // Booking Service
                .route("booking-service", r -> r.path("/booking-service/**")
                        .filters(f -> f.rewritePath("/booking-service/(?<segment>.*)", "/${segment}"))
                        .uri("lb://booking-service"))
                // Payment Service
                .route("review-service", r -> r.path("/review-service/**")
                        .filters(f -> f.rewritePath("/review-service/(?<segment>.*)", "/${segment}"))
                        .uri("lb://review-service"))
                // Hotel Service
                .route("hotel-service", r -> r.path("/hotel-service/**")
                        .filters(f -> f.rewritePath("/hotel-service/(?<segment>.*)", "/${segment}"))
                        .uri("lb://hotel-service"))
                //Room Service
                .route("room-service", r -> r.path("/room-service/**")
                        .filters(f -> f.rewritePath("/room-service/(?<segment>.*)", "/${segment}"))
                        .uri("lb://room-service"))
                .build();
    }
}