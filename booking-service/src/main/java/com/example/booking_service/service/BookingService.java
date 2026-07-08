package com.example.booking_service.service;

import com.example.booking_service.Iservice.IBookingService;
import com.example.booking_service.client.HotelServiceClient;
import com.example.booking_service.client.PaymentServiceClient;
import com.example.booking_service.client.UserServiceClient;
import com.example.booking_service.dto.request.AddBooking;
import com.example.booking_service.dto.request.AddBookingItem;
import com.example.booking_service.dto.request.BookingRequestEvent;
import com.example.booking_service.dto.request.CheckInCheckOut;
import com.example.booking_service.dto.request.Update;
import com.example.booking_service.dto.response.Bookings;
import com.example.booking_service.dto.response.Books;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingRequest;
import com.example.booking_service.model.BookingType;
import com.example.booking_service.model.CheckInOutAction;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.BookingRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService implements IBookingService {
    private static final long DUPLICATE_WINDOW_SECONDS = 15;
    private static final String BOOKING_TOPIC = "booking-request-topic";

    @Autowired
    private HotelServiceClient hotelServiceClient;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserServiceClient userServiceClient;
    @Autowired(required = false)
    private PaymentServiceClient paymentServiceClient;
    @Autowired
    private com.example.booking_service.client.RoomServiceClient roomServiceClient;
    @Autowired
    private BookingRequestRepository bookingRequestRepository;
    @Autowired
    private BookingStatusUpdater bookingStatusUpdater;
    @Autowired(required = false)
    private KafkaTemplate<String, BookingRequestEvent> bookingKafkaTemplate;

    public String submitBookingRequest(int customerId, AddBooking addBooking) {
        String requestId = UUID.randomUUID().toString();

        // Lưu trạng thái PENDING vào DB để FE có thể polling
        BookingRequest bookingRequest = BookingRequest.builder()
                .requestId(requestId)
                .customerId(customerId)
                .hotelId(addBooking.hotelId)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        bookingRequestRepository.save(bookingRequest);

        String partitionKey = buildPartitionKey(addBooking);

        BookingRequestEvent event = BookingRequestEvent.builder()
                .requestId(requestId)
                .customerId(customerId)
                .hotelId(addBooking.hotelId)
                .checkInDate(addBooking.checkInDate)
                .checkOutDate(addBooking.checkOutDate)
                .guests(addBooking.guests)
                .bookingType(addBooking.bookingType)
                .bookingItems(addBooking.bookingItems)
                .build();

        if (bookingKafkaTemplate != null) {
            bookingKafkaTemplate.send(BOOKING_TOPIC, partitionKey, event);
        } else {
            processBookingEvent(event);
        }

        return requestId;
    }

    public BookingRequest getBookingRequestStatus(String requestId) {
        return bookingRequestRepository.findById(requestId).orElse(null);
    }

    public void processBookingEvent(BookingRequestEvent event) {
        System.out.println("[BookingService] Bắt đầu xử lý requestId=" + event.getRequestId());
        try {
            Integer bookingId = addBooking(event.getCustomerId(), toAddBooking(event));
            bookingStatusUpdater.markSuccess(event.getRequestId(), bookingId);

        } catch (IllegalStateException e) {
            System.err.println("[BookingService] Business error requestId=" + event.getRequestId() + ": " + e.getMessage());
            bookingStatusUpdater.markFailed(event.getRequestId(), resolveErrorMessage(e.getMessage()));

        } catch (Exception e) {
            System.err.println("[BookingService] Unexpected error requestId=" + event.getRequestId() + ": " + e.getMessage());
            bookingStatusUpdater.markFailed(event.getRequestId(), "Đã xảy ra lỗi khi xử lý đặt phòng.");
        }
    }


    // Logic đặt phòng.
    private Integer addBookingInternal(int customerId, AddBooking addBooking) {
        if (addBooking == null) return null;

        Map<String, Object> response = hotelServiceClient.checkHotelExists(addBooking.hotelId);
        boolean hotelExists = (boolean) response.get("exists");
        if (!hotelExists) return null;

        if (addBooking.bookingItems == null || addBooking.bookingItems.isEmpty()) return null;

        List<Booking> overlapping = bookingRepository.findOverlappingActiveBookings(
                customerId,
                addBooking.hotelId,
                addBooking.checkInDate,
                addBooking.checkOutDate
        );
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("DUPLICATE_BOOKING");
        }

        Map<BookingItemKey, Integer> normalizedItems = normalizeBookingItems(addBooking.bookingItems);
        if (normalizedItems.isEmpty()) return null;

        //lấy danh sách phòng của khách sạn
        Map<String, Object> roomResponse = roomServiceClient.getRoomsByHotelId(addBooking.hotelId);
        List<Map<String, Object>> roomInventory = new ArrayList<>();
        if (roomResponse != null && roomResponse.containsKey("rooms")) {
            Object roomsObj = roomResponse.get("rooms");
            if (roomsObj instanceof List) {
                roomInventory = (List<Map<String, Object>>) roomsObj;
            }
        }

        // Kiểm tra overbooking cho từng loại phòng khách muốn đặt
        for (Map.Entry<BookingItemKey, Integer> entry : normalizedItems.entrySet()) {
            String roomType = entry.getKey().roomType();
            int requestedCount = entry.getValue();

            int totalRoomsCapacity = 0;
            for (Map<String, Object> roomMap : roomInventory) {
                if (roomType.equalsIgnoreCase((String) roomMap.get("roomType"))) {
                    Object totalRoomsObj = roomMap.get("totalRooms");
                    if (totalRoomsObj instanceof Number) {
                        totalRoomsCapacity = ((Number) totalRoomsObj).intValue();
                    } else if (totalRoomsObj instanceof String) {
                        totalRoomsCapacity = Integer.parseInt((String) totalRoomsObj);
                    }
                    break;
                }
            }

            if (totalRoomsCapacity <= 0) {
                throw new IllegalStateException("ROOM_NOT_FOUND: " + roomType);
            }

            // Đếm số lượng phòng bị trùng lặp
            List<Booking> overlappingBookings = bookingRepository.findBookedRoomsForDateRange(
                    addBooking.hotelId,
                    roomType,
                    addBooking.checkInDate,
                    addBooking.checkOutDate
            );

            int bookedCount = 0;
            for (Booking b : overlappingBookings) {
                bookedCount += b.getTotalRoom();
            }

            if (bookedCount + requestedCount > totalRoomsCapacity) {
                throw new IllegalStateException("ROOM_SOLD_OUT: " + roomType);
            }
        }

        LocalDateTime createdAt = LocalDateTime.now();
        Integer duplicatedBookingId = findDuplicatedBookingId(customerId, addBooking, normalizedItems, createdAt);
        if (duplicatedBookingId != null) {
            return duplicatedBookingId;
        }

        // Tính số đêm từ checkInDate → checkOutDate
        long nights = Math.max(1,
                addBooking.checkOutDate.toLocalDate().toEpochDay()
                - addBooking.checkInDate.toLocalDate().toEpochDay());

        List<Booking> bookings = new ArrayList<>();
        for (Map.Entry<BookingItemKey, Integer> entry : normalizedItems.entrySet()) {
            BookingItemKey item = entry.getKey();
            int totalRoom = entry.getValue();
            // totalFee = giá 1 đêm × số phòng × số đêm
            float totalFee = item.unitFee() * totalRoom * nights;

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
        if (savedBookings.isEmpty()) return null;
        return savedBookings.get(0).getId();
    }


    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Integer addBooking(int customerId, AddBooking addBooking) {
        return addBookingInternal(customerId, addBooking);
    }

    private String buildPartitionKey(AddBooking addBooking) {
        // Key bao gồm hotelId để đảm bảo cùng khách sạn xếp hàng cùng partition
        StringBuilder sb = new StringBuilder(String.valueOf(addBooking.hotelId));
        if (addBooking.bookingItems != null) {
            for (AddBookingItem item : addBooking.bookingItems) {
                if (item != null && item.roomType != null) {
                    sb.append(":").append(item.roomType.trim().toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    private AddBooking toAddBooking(BookingRequestEvent event) {
        AddBooking addBooking = new AddBooking();
        addBooking.hotelId = event.getHotelId();
        addBooking.checkInDate = event.getCheckInDate();
        addBooking.checkOutDate = event.getCheckOutDate();
        addBooking.guests = event.getGuests();
        addBooking.bookingType = event.getBookingType();
        addBooking.bookingItems = event.getBookingItems();
        return addBooking;
    }

    private String resolveErrorMessage(String exceptionMessage) {
        if (exceptionMessage == null) return "Đã xảy ra lỗi không xác định.";
        if ("DUPLICATE_BOOKING".equals(exceptionMessage)) {
            return "Bạn đã có đặt phòng tại khách sạn này trong khoảng thời gian đó.";
        }
        if (exceptionMessage.startsWith("ROOM_SOLD_OUT:")) {
            return "Đã hết phòng trong thời gian bạn đặt.";
        }
        if (exceptionMessage.startsWith("ROOM_NOT_FOUND:")) {
            return "Đã hết phòng trong thời gian bạn đặt.";
        }
        return "Đã xảy ra lỗi khi xử lý đặt phòng.";
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

        if (recentBookings.isEmpty()) return null;

        Map<String, Integer> minimumIdByItem = new HashMap<>();
        for (Booking booking : recentBookings) {
            String itemKey = toSavedItemKey(booking.getRoomType(), booking.getTotalRoom(), booking.getFee());
            minimumIdByItem.merge(itemKey, booking.getId(), Math::min);
        }

        Integer duplicatedBookingId = null;
        //Duyệt phòng khách yêu cầu
        for (Map.Entry<BookingItemKey, Integer> entry : normalizedItems.entrySet()) {
            BookingItemKey item = entry.getKey();
            int totalRoom = entry.getValue();
            float totalFee = item.unitFee() * totalRoom;
            String expectedItemKey = toSavedItemKey(item.roomType(), totalRoom, totalFee);
            Integer itemId = minimumIdByItem.get(expectedItemKey);
            if (itemId == null) return null;
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

            boolean isPaid = false;
            if (paymentServiceClient != null) {
                try {
                    Map<String, Object> paymentStatus = paymentServiceClient.getPaymentStatusByBookingId(booking.getId());
                    if (paymentStatus != null) {
                        Object paidValue = paymentStatus.get("isPaid");
                        if (paidValue instanceof Boolean) {
                            isPaid = (Boolean) paidValue;
                        } else if (paidValue != null) {
                            isPaid = Boolean.parseBoolean(String.valueOf(paidValue));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Cannot fetch payment status for bookingId=" + booking.getId() + ": " + e.getMessage());
                }
            }

            Bookings bookingResponse = Bookings.builder()
                    .id(booking.getId())
                    .hotelId(booking.getHotelId())
                    .hotelName(hotelName)
                    .address(address)
                    .imageUrl(imageUrl)
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .guests(booking.getGuests())
                    .status(resolveStatus(booking.getBookingType()))
                    .fee(booking.getFee())
                    .isPaid(isPaid)
                    .build();
            books.add(bookingResponse);
        }
        return books;
    }

    @Override
    public int getBookedRooms(int hotelId, String checkInDate, String checkOutDate) {
        try {
            Date checkIn = Date.valueOf(checkInDate);
            Date checkOut = Date.valueOf(checkOutDate);
            List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(hotelId, checkIn, checkOut);
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
        if (booking == null) return false;
        booking.setBookingType(update.status);
        bookingRepository.save(booking);
        return true;
    }

    @Override
    public List<Books> getBookingsByHotelId(int hotelId) {
        List<Booking> bookings = bookingRepository.findByHotelId(hotelId);
        List<Books> bookingsResponse = new ArrayList<>();
        for (Booking booking : bookings) {
            String customerName = "Unknown";
            String phone = "";
            Map<String, Object> userResponse = userServiceClient.Customer(booking.getCustomerId());
            if (userResponse != null) {
                Object fullNameValue = userResponse.get("fullName");
                Object phoneValue = userResponse.get("phone");
                if (fullNameValue != null) customerName = String.valueOf(fullNameValue);
                if (phoneValue != null) phone = String.valueOf(phoneValue);
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
        if (bookingType == null) return "UNKNOWN";
        if (bookingType == BookingType.CHECKED_OUT) return BookingType.COMPLETED.name();
        return bookingType.name();
    }

    @Override
    public boolean checkIntOutHotel(CheckInCheckOut checkInCheckOut) {
        if (checkInCheckOut == null || checkInCheckOut.bookingId <= 0 || checkInCheckOut.action == null) {
            return false;
        }
        Booking booking = bookingRepository.findById(checkInCheckOut.bookingId).orElse(null);
        if (booking == null) return false;

        if (checkInCheckOut.action == CheckInOutAction.CHECKED_IN) {
            if (booking.getBookingType() != BookingType.CONFIRMED) return false;
            booking.setBookingType(BookingType.CHECKED_IN);
        } else if (checkInCheckOut.action == CheckInOutAction.CHECKED_OUT) {
            if (booking.getBookingType() != BookingType.CHECKED_IN) return false;
            booking.setBookingType(BookingType.COMPLETED);
        } else {
            return false;
        }

        bookingRepository.save(booking);
        return true;
    }
}
