package com.chat_service.chat_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/ownerId")
    Map<String, Object> getOwnerId(@RequestParam("hotelId") int hotelId);

    @GetMapping("/hotelId")
    Map<String, Object> getHotelId(@RequestParam("userId") int userId);
}
