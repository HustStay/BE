package com.example.booking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
@EnableFeignClients
public class BookingServiceApplication {

	public static void main(String[] args) {

        SpringApplication.run(BookingServiceApplication.class, args);
        System.out.println("Booking Service is running...");
    }

}
