package com.hotel.hotel_service.service;

import com.hotel.hotel_service.Iservice.IHotelService;
import com.hotel.hotel_service.client.ReviewServiceClient;
import com.hotel.hotel_service.client.RoomServiceClient;
import com.hotel.hotel_service.dto.request.SearchHotel;
import com.hotel.hotel_service.dto.response.HotelDetail;
import com.hotel.hotel_service.dto.response.Hotels;
import com.hotel.hotel_service.dto.response.SearchHotelResult;
import com.hotel.hotel_service.model.Amenities;
import com.hotel.hotel_service.model.Home;
import com.hotel.hotel_service.model.HomeAmeneties;
import com.hotel.hotel_service.model.HomeImage;
import com.hotel.hotel_service.repository.AmenityRepository;
import com.hotel.hotel_service.repository.HomeAmenityRepository;
import com.hotel.hotel_service.repository.HomeImageRepository;
import com.hotel.hotel_service.repository.HomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HotelService implements IHotelService {

    @Autowired
    private HomeRepository homeRepository;
    @Autowired
    private AmenityRepository amenityRepository;
    @Autowired
    private HomeAmenityRepository homeAmenityRepository;
    @Autowired
    private HomeImageRepository homeImageRepository;
    @Autowired
    private RoomServiceClient roomServiceClient;

    @Override
    public List<Hotels> getFamousHotels() {
        List<Hotels> hotels=new ArrayList<>();
        List<Home> homes = homeRepository.findByRatingGreaterThan(4.0);
        if (homes.isEmpty()) {
            return null;
        }

        for (Home home : homes) {
            Hotels hotelResponse = new Hotels();
            hotelResponse.hotelId = home.getId();
            hotelResponse.hotelName = home.getHome_name();
            hotelResponse.pricePerNight = home.getPrice_per_night();
            hotelResponse.rating = home.getRating();
            hotelResponse.street = home.getStreet();
            hotelResponse.district = home.getDistrict();
            hotelResponse.city = home.getCity();
            hotelResponse.country = home.getCountry();

            List<String> amenitiesList = new ArrayList<>();
            List<HomeAmeneties> homeAmenetiesList = homeAmenityRepository.findHomeAmenetiesByHome_Id(home.getId());
            if (homeAmenetiesList.isEmpty()) {
                return null;
            }

            for (HomeAmeneties homeAmeneties : homeAmenetiesList) {
                List<Amenities> amenities = amenityRepository.findAnimitiesById(homeAmeneties.getAmenity().getId());
                if (amenities.isEmpty()) {
                    return null;
                }
                for (Amenities amenity : amenities) {
                    amenitiesList.add(amenity.getAmenity_name());
                }
            }
            hotelResponse.aminities = String.join(", ", amenitiesList);

            List<HomeImage> homeImages = homeImageRepository.findHomeImageByHome_Id(home.getId());
            if (homeImages.isEmpty()) {
                hotelResponse.imageUrl = "";
            } else {
                hotelResponse.imageUrl = homeImages.get(0).getImageUrl();
            }

            hotels.add(hotelResponse);
        }
        return hotels;
    }

    @Override
    public List<Hotels> getAllHotels() {
        List<Hotels> hotels=new ArrayList<>();
        List<Home> homes = homeRepository.findAll();
        if (homes.isEmpty()) {
            return null;
        }

        for (Home home : homes) {
            Hotels hotelResponse = new Hotels();
            hotelResponse.hotelId = home.getId();
            hotelResponse.hotelName = home.getHome_name();
            hotelResponse.pricePerNight = home.getPrice_per_night();
            hotelResponse.rating = home.getRating();
            hotelResponse.street = home.getStreet();
            hotelResponse.district = home.getDistrict();
            hotelResponse.city = home.getCity();
            hotelResponse.country = home.getCountry();

            List<String> amenitiesList = new ArrayList<>();
            List<HomeAmeneties> homeAmenetiesList = homeAmenityRepository.findHomeAmenetiesByHome_Id(home.getId());
            if (homeAmenetiesList.isEmpty()) {
                return null;
            }

            for (HomeAmeneties homeAmeneties : homeAmenetiesList) {
                List<Amenities> amenities = amenityRepository.findAnimitiesById(homeAmeneties.getAmenity().getId());
                if (amenities.isEmpty()) {
                    return null;
                }
                for (Amenities amenity : amenities) {
                    amenitiesList.add(amenity.getAmenity_name());
                }
            }
            hotelResponse.aminities = String.join(", ", amenitiesList);

            List<HomeImage> homeImages = homeImageRepository.findHomeImageByHome_Id(home.getId());
            if (homeImages.isEmpty()) {
                hotelResponse.imageUrl = "";
            } else {
                hotelResponse.imageUrl = homeImages.get(0).getImageUrl();
            }

            hotels.add(hotelResponse);
        }
        return hotels;
    }

    @Override
    public HotelDetail getHotelDetail(int hotelId) {
        Optional<Home> hotelOptional = homeRepository.findById(hotelId);
        if (hotelOptional.isPresent()) {
            HotelDetail hotelDetail = new HotelDetail();
            Home home = hotelOptional.get();
            hotelDetail.hotelName = home.getHome_name();
            hotelDetail.rating = home.getRating();
            hotelDetail.street = home.getStreet();
            hotelDetail.district = home.getDistrict();
            hotelDetail.city = home.getCity();
            hotelDetail.country = home.getCountry();

            List<String> amenitiesList = new ArrayList<>();
            List<HomeAmeneties> homeAmenetiesList = homeAmenityRepository.findHomeAmenetiesByHome_Id(hotelId);
            if (homeAmenetiesList.isEmpty()) {
                hotelDetail.aminities = String.join(", ", "");
            }
            for (HomeAmeneties homeAmeneties : homeAmenetiesList) {
                List<Amenities> amenities = amenityRepository.findAnimitiesById(homeAmeneties.getAmenity().getId());
                if (!amenities.isEmpty()) {
                    for (Amenities amenity : amenities) {
                        amenitiesList.add(amenity.getAmenity_name());
                    }
                }
            }
            hotelDetail.aminities = String.join(", ", amenitiesList);

            List<String> images = new ArrayList<>();
            List<HomeImage> homeImages = homeImageRepository.findHomeImageByHome_Id(hotelId);
            if (homeImages.isEmpty()) {
                hotelDetail.imageUrl = String.join(", ", "");
            }
            for (HomeImage homeImage : homeImages) {
                if(homeImage.getImageUrl() != null) {
                    images.add(homeImage.getImageUrl());
                }
            }
            hotelDetail.imageUrl = String.join(", ", images);

            return hotelDetail;
        }
        return null;
    }

//    @Override
//    public List<Hotels> getHotelsSearch(SearchHotel searchHotel) {
//        List<Home> homes = homeRepository.findByCityContainingIgnoreCase(searchHotel.city);
//        if (homes.isEmpty()) {
//            return null;
//        }
//        List<Hotels> hotels = new ArrayList<>();
//        for (Home home : homes) {
//            Hotels hotelResponse = new Hotels();
//            hotelResponse.hotelId = home.getId();
//            hotelResponse.hotelName = home.getHome_name();
//            hotelResponse.pricePerNight = home.getPrice_per_night();
//            hotelResponse.rating = home.getRating();
//            hotelResponse.street = home.getStreet();
//            hotelResponse.district = home.getDistrict();
//            hotelResponse.city = home.getCity();
//            hotelResponse.country = home.getCountry();
//
//            List<String> amenitiesList = new ArrayList<>();
//            List<HomeAmeneties> homeAmenetiesList = homeAmenityRepository.findHomeAmenetiesByHome_Id(home.getId());
//            for (HomeAmeneties homeAmeneties : homeAmenetiesList) {
//                List<Amenities> amenities = amenityRepository.findAnimitiesById(homeAmeneties.getAmenity().getId());
//                for (Amenities amenity : amenities) {
//                    amenitiesList.add(amenity.getAmenity_name());
//                }
//            }
//            hotelResponse.aminities = String.join(", ", amenitiesList);
//
//            List<HomeImage> homeImages = homeImageRepository.findHomeImageByHome_Id(home.getId());
//            if (!homeImages.isEmpty()) {
//                hotelResponse.imageUrl = homeImages.get(0).getImageUrl();
//            }
//
//            hotels.add(hotelResponse);
//        }
//        return hotels;
//    }

    @Override
    public List<SearchHotelResult> searchHotels(SearchHotel searchHotel) {
        // 1. Tìm khách sạn theo thành phố
        List<Home> homes = homeRepository.findByCityContainingIgnoreCase(searchHotel.city);
        if (homes.isEmpty()) {
            return new ArrayList<>();
        }

        List<SearchHotelResult> results = new ArrayList<>();

        for (Home home : homes) {
            try {
                Map<String, Object> roomResponse = roomServiceClient.getAvailableRoomCount(
                    home.getId(),
                    searchHotel.checkInDate,
                    searchHotel.checkOutDate,
                    searchHotel.numberOfGuests,
                    searchHotel.numberOfRooms
                );

                Integer availableRooms = (Integer) roomResponse.get("availableRooms");
                Integer totalRooms = (Integer) roomResponse.get("totalRooms");

                if (availableRooms != null && availableRooms >= searchHotel.numberOfRooms) {
                    SearchHotelResult result = new SearchHotelResult();
                    result.setHotelId(home.getId());
                    result.setHotelName(home.getHome_name());
                    result.setStreet(home.getStreet());
                    result.setDistrict(home.getDistrict());
                    result.setCity(home.getCity());
                    result.setCountry(home.getCountry());
                    result.setRating(home.getRating());
                    result.setAvailableRooms(availableRooms);
                    result.setTotalRooms(totalRooms != null ? totalRooms : 0);

                    // Lấy hình ảnh
                    List<HomeImage> homeImages = homeImageRepository.findHomeImageByHome_Id(home.getId());
                    if (!homeImages.isEmpty()) {
                        result.setImageUrl(homeImages.get(0).getImageUrl());
                    } else {
                        result.setImageUrl("");
                    }

                    results.add(result);
                }
            } catch (Exception e) {
                // Log error và tiếp tục với khách sạn tiếp theo
                System.err.println("Error checking availability for hotel " + home.getId() + ": " + e.getMessage());
            }
        }

        return results;
    }

    @Override
    public boolean existsById(int hotelId) {
        Optional<Home> home = homeRepository.findById(hotelId);
        if (home.isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public HotelDetail getHotelDetailForBooking(int hotelId) {
        Optional<Home> hotelOptional = homeRepository.findById(hotelId);
        if (hotelOptional.isPresent()) {
            HotelDetail hotelDetail = new HotelDetail();
            Home home = hotelOptional.get();
            hotelDetail.hotelName = home.getHome_name();
            hotelDetail.street = home.getStreet();
            hotelDetail.district = home.getDistrict();
            hotelDetail.city = home.getCity();
            hotelDetail.country = home.getCountry();

            List<HomeImage> homeImages = homeImageRepository.findHomeImageByHome_Id(home.getId());
            if (homeImages.isEmpty()) {
                hotelDetail.imageUrl = "";
            } else {
                hotelDetail.imageUrl = homeImages.get(0).getImageUrl();
            }

            return hotelDetail;
        }
        return null;
    }

    @Override
    public int getHotelId(int userId) {
        Optional<Home> home = homeRepository.findByOwnerId(userId);
        if (home.isPresent()) {
            return home.get().getId();
        }
        return 0;
    }

    @Override
    public int getOwnerIdByHotelId(int hotelId) {
        Optional<Home> home = homeRepository.findById(hotelId);
        if (home.isPresent()) {
            return home.get().getOwnerId();
        }
        return 0;
    }


}
