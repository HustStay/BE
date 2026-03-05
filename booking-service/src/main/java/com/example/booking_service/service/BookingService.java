package com.example.booking_service.service;

import com.example.booking_service.Iservice.IBookingService;
import com.example.booking_service.client.HotelServiceClient;
import com.example.booking_service.client.UserServiceClient;
import com.example.booking_service.dto.request.AddBooking;
import com.example.booking_service.dto.request.AddBookingItem;
import com.example.booking_service.dto.request.CheckInCheckOut;
import com.example.booking_service.dto.request.Update;
import com.example.booking_service.dto.response.Bookings;
import com.example.booking_service.dto.response.Books;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingType;
import com.example.booking_service.model.CheckInOutAction;
import com.example.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService implements IBookingService {
    @Autowired
    private HotelServiceClient hotelServiceClient;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    public boolean addBooking(int customerId, AddBooking addBooking) {
        Map<String, Object> response = hotelServiceClient.checkHotelExists(addBooking.hotelId);
        boolean hotelExists = (boolean) response.get("exists");
        if (!hotelExists) {
            return false;
        }

        if (addBooking.bookingItems == null || addBooking.bookingItems.isEmpty()) {
            return false;
        }

        List<Booking> bookings = new ArrayList<>();

        for (AddBookingItem item : addBooking.bookingItems) {
            float totalFee = item.fee * item.totalRoom;

            Booking booking = Booking.builder()
                    .customerId(customerId)
                    .hotelId(addBooking.hotelId)
                    .checkInDate(addBooking.checkInDate)
                    .checkOutDate(addBooking.checkOutDate)
                    .guests(addBooking.guests)
                    .bookingType(addBooking.bookingType)
                    .roomType(item.roomType)
                    .totalRoom(item.totalRoom)
                    .fee(totalFee)
                    .createdAt(LocalDateTime.now())
                    .build();

            bookings.add(booking);
        }
        bookingRepository.saveAll(bookings);
        return true;
    }

    @Override
    public List<Bookings> getBookingsByCustomerId(int customerId) {
        List<Bookings> books = new ArrayList<>();
        List<Booking> bookings = bookingRepository.findByCustomerId(customerId);
        if (bookings.isEmpty()) {
            return null;
        }
        for (Booking booking : bookings) {
            Map<String, Object> response = hotelServiceClient.getHotelDetail(booking.getHotelId());
            String hotelName = (String) response.get("hotelName");
            String address = response.get("street") + ", " +
                             response.get("district") + ", " +
                             response.get("city") + ", " +
                             response.get("country");
            String imageUrl = (String) response.get("imageUrl");
            Bookings bookingResponse = Bookings.builder()
                    .id(booking.getId())
                    .hotelName(hotelName)
                    .address(address)
                    .imageUrl(imageUrl)
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .guests(booking.getGuests())
                    .status(booking.getBookingType().name())
                    .build();
            books.add(bookingResponse);
        }
        return books;
    }

    @Override
    public int getBookedRooms(int hotelId, String checkInDate, String checkOutDate) {
        try {
            // Convert String to Date
            Date checkIn = Date.valueOf(checkInDate);
            Date checkOut = Date.valueOf(checkOutDate);

            // Tìm các booking conflict với khoảng thời gian
            List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                hotelId, checkIn, checkOut
            );

            // Tính tổng số phòng đã đặt
            int totalBookedRooms = 0;
            for (Booking booking : conflictingBookings) {
                totalBookedRooms += booking.getTotalRoom();
            }

            return totalBookedRooms;
        } catch (Exception e) {
            System.err.println("Error getting booked rooms: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean updateBookingStatus(Update update) {
        Booking booking = bookingRepository.findById(update.bookingId).orElse(null);
        if (booking == null) {
            return false;
        }
        booking.setBookingType(update.status);
        bookingRepository.save(booking);
        return true;
    }

    @Override
    public List<Books> getBookingsByHotelId(int ownerId) {
        Map<String, Object> response = hotelServiceClient.checkHotelId(ownerId);
        int hotelId = (int) response.get("hotelId");

        List<Booking> bookings = bookingRepository.findByHotelId(hotelId);
        List<Books> bookingsResponse = new ArrayList<>();
        for (Booking booking : bookings){
            Map<String,Object> userResponse = userServiceClient.Customer(booking.getCustomerId());
            String customerName = (String) userResponse.get("fullName");
            String phone = (String) userResponse.get("phone");
            Books book = Books.builder()
                    .id(booking.getId())
                    .customerName(customerName)
                    .phone(phone)
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .guestCount(booking.getGuests())
                    .status(booking.getBookingType().name())
                    .totalRooms(booking.getTotalRoom())
                    .roomType(booking.getRoomType())
                    .fee(booking.getFee())
                    .build();
            bookingsResponse.add(book);
        }
        return bookingsResponse;
    }

    @Override
    public boolean checkIntOutHotel(CheckInCheckOut checkInCheckOut) {
        if (checkInCheckOut == null || checkInCheckOut.bookingId <= 0 || checkInCheckOut.action == null) {
            return false;
        }

        Booking booking = bookingRepository.findById(checkInCheckOut.bookingId).orElse(null);
        if (booking == null) {
            return false;
        }

        if (checkInCheckOut.action == CheckInOutAction.CHECKED_IN) {
            if (booking.getBookingType() != BookingType.CONFIRMED) {
                return false;
            }
            booking.setBookingType(BookingType.CHECKED_IN);
        } else if (checkInCheckOut.action == CheckInOutAction.CHECKED_OUT) {
            // Chỉ cho phép check-out nếu đã check-in
            if (booking.getBookingType() != BookingType.CHECKED_IN) {
                return false;
            }
            booking.setBookingType(BookingType.COMPLETED);
        } else {
            return false;
        }

        bookingRepository.save(booking);
        return true;
    }
}
