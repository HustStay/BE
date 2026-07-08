package com.example.booking_service.dto.request;

import com.example.booking_service.model.BookingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequestEvent {
    private String requestId;
    private int customerId;
    private int hotelId;
    private Date checkInDate;
    private Date checkOutDate;
    private int guests;
    private BookingType bookingType;
    private List<AddBookingItem> bookingItems;
}
