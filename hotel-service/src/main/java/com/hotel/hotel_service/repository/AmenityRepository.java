package com.hotel.hotel_service.repository;

import com.hotel.hotel_service.model.Amenities;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRepository extends JpaRepository<Amenities,Integer> {
    List<Amenities> findById(int id);

    List<Amenities> findAnimitiesById(int id);
}
