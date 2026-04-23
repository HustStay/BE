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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService implements IBookingService {
    private static final long DUPLICATE_WINDOW_SECONDS = 15;

    @Autowired
    private HotelServiceClient hotelServiceClient;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    public Integer addBooking(int customerId, AddBooking addBooking) {
        if (addBooking == null) {
            return null;
        }

        Map<String, Object> response = hotelServiceClient.checkHotelExists(addBooking.hotelId);
        boolean hotelExists = (boolean) response.get("exists");
        if (!hotelExists) {
            return null;
        }

        if (addBooking.bookingItems == null || addBooking.bookingItems.isEmpty()) {
            return null;
        }

        Map<BookingItemKey, Integer> normalizedItems = normalizeBookingItems(addBooking.bookingItems);
        if (normalizedItems.isEmpty()) {
            return null;
        }

        LocalDateTime createdAt = LocalDateTime.now();
        Integer duplicatedBookingId = findDuplicatedBookingId(customerId, addBooking, normalizedItems, createdAt);
        if (duplicatedBookingId != null) {
            return duplicatedBookingId;
        }

        List<Booking> bookings = new ArrayList<>();

        for (Map.Entry<BookingItemKey, Integer> entry : normalizedItems.entrySet()) {
            BookingItemKey item = entry.getKey();
            int totalRoom = entry.getValue();
            float totalFee = item.unitFee() * totalRoom;

            Booking booking = Booking.builder()
                    .customerId(customerId)
                    .hotelId(addBooking.hotelId)
                    .checkInDate(addBooking.checkInDate)
                    .checkOutDate(addBooking.checkOutDate)
                    .guests(addBooking.guests)
                    .bookingType(addBooking.bookingType)
                    .roomType(item.roomType())
                    .totalRoom(totalRoom)
                    .fee(totalFee)
                    .createdAt(createdAt)
                    .build();

            bookings.add(booking);
        }
        List<Booking> savedBookings = bookingRepository.saveAll(bookings);
        if (savedBookings.isEmpty()) {
            return null;
        }
        return savedBookings.get(0).getId();
    }

    private Integer findDuplicatedBookingId(
            int customerId,
            AddBooking addBooking,
            Map<BookingItemKey, Integer> normalizedItems,
            LocalDateTime now
    ) {
        List<Booking> recentBookings = bookingRepository.findRecentSameRequestBookings(
                customerId,
                addBooking.hotelId,
                addBooking.checkInDate,
                addBooking.checkOutDate,
                addBooking.guests,
                addBooking.bookingType,
                now.minusSeconds(DUPLICATE_WINDOW_SECONDS)
        );

        if (recentBookings.isEmpty()) {
            return null;
        }

        Map<String, Integer> minimumIdByItem = new HashMap<>();
        for (Booking booking : recentBookings) {
            String itemKey = toSavedItemKey(booking.getRoomType(), booking.getTotalRoom(), booking.getFee());
            minimumIdByItem.merge(itemKey, booking.getId(), Math::min);
        }

        Integer duplicatedBookingId = null;
        for (Map.Entry<BookingItemKey, Integer> entry : normalizedItems.entrySet()) {
            BookingItemKey item = entry.getKey();
            int totalRoom = entry.getValue();
            float totalFee = item.unitFee() * totalRoom;
            String expectedItemKey = toSavedItemKey(item.roomType(), totalRoom, totalFee);
            Integer itemId = minimumIdByItem.get(expectedItemKey);
            if (itemId == null) {
                return null;
            }
            duplicatedBookingId = duplicatedBookingId == null ? itemId : Math.min(duplicatedBookingId, itemId);
        }

        return duplicatedBookingId;
    }

    private Map<BookingItemKey, Integer> normalizeBookingItems(List<AddBookingItem> bookingItems) {
        Map<BookingItemKey, Integer> normalized = new LinkedHashMap<>();
        for (AddBookingItem item : bookingItems) {
            if (item == null || item.totalRoom <= 0 || item.roomType == null || item.roomType.isBlank()) {
                continue;
            }
            BookingItemKey key = new BookingItemKey(item.roomType.trim(), item.fee);
            normalized.merge(key, item.totalRoom, Integer::sum);
        }
        return normalized;
    }

    private String toSavedItemKey(String roomType, int totalRoom, float totalFee) {
        return roomType + "|" + totalRoom + "|" + Float.floatToIntBits(totalFee);
    }

    private record BookingItemKey(String roomType, float unitFee) {}

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
                    .status(resolveStatus(booking.getBookingType()))
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
    public List<Books> getBookingsByHotelId(int hotelId) {
        List<Booking> bookings = bookingRepository.findByHotelId(hotelId);
        List<Books> bookingsResponse = new ArrayList<>();
        for (Booking booking : bookings){
            String customerName = "Unknown";
            String phone = "";
                Map<String,Object> userResponse = userServiceClient.Customer(booking.getCustomerId());
                if (userResponse != null) {
                    Object fullNameValue = userResponse.get("fullName");
                    Object phoneValue = userResponse.get("phone");
                    if (fullNameValue != null) {
                        customerName = String.valueOf(fullNameValue);
                    }
                    if (phoneValue != null) {
                        phone = String.valueOf(phoneValue);
                    }
                }
            Books book = Books.builder()
                    .id(booking.getId())
                    .customerName(customerName)
                    .phone(phone)
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .guestCount(booking.getGuests())
                    .status(resolveStatus(booking.getBookingType()))
                    .totalRooms(booking.getTotalRoom())
                    .roomType(booking.getRoomType())
                    .fee(booking.getFee())
                    .build();
            bookingsResponse.add(book);
        }
        return bookingsResponse;
    }

    private String resolveStatus(BookingType bookingType) {
        if (bookingType == null) {
            return "UNKNOWN";
        }
        if (bookingType == BookingType.CHECKED_OUT) {
            return BookingType.COMPLETED.name();
        }
        return bookingType.name();
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
