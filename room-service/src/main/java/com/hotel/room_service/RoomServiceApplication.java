package com.hotel.room_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class RoomServiceApplication {

	public static void main(String[] args) {

        SpringApplication.run(RoomServiceApplication.class, args);
        System.out.println("Room Service is running...");
	}

}
