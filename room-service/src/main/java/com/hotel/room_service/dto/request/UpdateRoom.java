package com.hotel.room_service.dto.request;

import lombok.Builder;

@Builder
public class UpdateRoom {
    public Integer roomId;
    public String roomType;
    public Double roomPrice;
    public Integer roomCapacity;
    public String roomDescription;
}

