package com.example.booking_service.repository;

import com.example.booking_service.dto.response.Bookings;
import com.example.booking_service.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByCustomerId(int customerId);

    @Query("SELECT b FROM Booking b WHERE b.hotelId = :hotelId AND " +
           "((b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate))")
    List<Booking> findConflictingBookings(
        @Param("hotelId") int hotelId,
        @Param("checkInDate") Date checkInDate,
        @Param("checkOutDate") Date checkOutDate
    );

    List<Booking> findByHotelId(int hotelId);
}
