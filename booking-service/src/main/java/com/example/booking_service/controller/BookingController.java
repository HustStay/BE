package com.example.booking_service.controller;

import com.example.booking_service.dto.request.AddBooking;
import com.example.booking_service.dto.request.CheckInCheckOut;
import com.example.booking_service.dto.request.Update;
import com.example.booking_service.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequestMapping
@RestController
public class BookingController {

    private final BookingService bookingService;
    
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/booking")
    public ResponseEntity<Map<String,Object>> booking(@RequestHeader("X-Auth-UserId") String userIdStr,
                                                      @RequestBody AddBooking booking) {
        Map<String,Object> response = new HashMap<>();
        int customerId = Integer.parseInt(userIdStr);
        try {
            // Only send Kafka message if KafkaTemplate is available
            if (kafkaTemplate != null) {
                // Tạo booking event để gửi qua Kafka
                Map<String, Object> bookingEvent = new HashMap<>();
                bookingEvent.put("customerId", customerId);
                bookingEvent.put("hotelId", booking.hotelId);
                bookingEvent.put("checkInDate", booking.checkInDate.toString());
                bookingEvent.put("checkOutDate", booking.checkOutDate.toString());
                bookingEvent.put("guests", booking.guests);
                bookingEvent.put("bookingType", booking.bookingType.toString());
                bookingEvent.put("bookingItems", booking.bookingItems);
                
                // Gửi message đến Kafka topic
                kafkaTemplate.send("booking-topic", String.valueOf(customerId), bookingEvent);
            }
            
            // Xử lý booking trong service
            Integer bookingId = bookingService.addBooking(customerId, booking);
            if (bookingId != null) {
                response.put("message","Booking successfully");
                response.put("bookingId", bookingId);
            }else {
                response.put("message","Booking failed");
            }
            return ResponseEntity.ok(response);
        }catch(Exception e){
            response.put("message","Error occurred during booking");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<Map<String,Object>> getBookingsByCustomerId(@RequestHeader("X-Auth-UserId") String userIdStr) {
        Map<String, Object> response = new HashMap<>();
        int customerId = Integer.parseInt(userIdStr);
        try {
            var bookings = bookingService.getBookingsByCustomerId(customerId);
            if (bookings != null && !bookings.isEmpty()) {
                response.put("message", "Get bookings successfully");
                response.put("bookings", bookings);
            } else {
                response.put("message", "No bookings found");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Error occurred while retrieving bookings");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/bookings/booked-rooms")
    public ResponseEntity<Map<String, Object>> getBookedRooms(
            @RequestParam("hotelId") int hotelId,
            @RequestParam("checkInDate") String checkInDate,
            @RequestParam("checkOutDate") String checkOutDate) {
        Map<String, Object> response = new HashMap<>();
        try {
            int bookedRooms = bookingService.getBookedRooms(hotelId, checkInDate, checkOutDate);
            response.put("bookedRooms", bookedRooms);
            response.put("hotelId", hotelId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("bookedRooms", 0);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/bookings/status")
    public ResponseEntity<Map<String, Object>> updateBookingStatus(@RequestBody Update update) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean updated = bookingService.updateBookingStatus(update);
            if (updated) {
                response.put("message", "Booking status updated successfully");
            } else {
                response.put("message", "Failed to update booking status");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Error occurred while updating booking status");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/owner/bookings" )
    public ResponseEntity<Map<String,Object>> getBookingsByHotelOwnerId(@RequestHeader("X-Auth-UserId") String userIdStr) {
        Map<String, Object> response = new HashMap<>();
        int ownerId = Integer.parseInt(userIdStr);
        try {
            var bookings = bookingService.getBookingsByHotelId(ownerId);
            if (bookings != null && !bookings.isEmpty()) {
                response.put("message", "Get bookings successfully");
                response.put("bookings", bookings);
            } else {
                response.put("message", "No bookings found");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Error occurred while retrieving bookings");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/checkInCheckOut")
    public ResponseEntity<Map<String,Object>> checkInCheckOut(@RequestBody CheckInCheckOut checkInCheckOut) {
        Map<String,Object> response = new HashMap<>();
        try {
            boolean check = bookingService.checkIntOutHotel(checkInCheckOut);
            if (check) {
                response.put("message", "Check-in/Check-out successful");
            }
            else {
                response.put("message", "Check-in/Check-out failed");
            }
            return ResponseEntity.ok(response);
        }catch (Exception e){
            response.put("message","Error occurred during Check-in/Check-out");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
