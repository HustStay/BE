package com.hotel.room_service.controller;

import com.hotel.room_service.dto.request.AddRoom;
import com.hotel.room_service.dto.request.UpdateActive;
import com.hotel.room_service.dto.request.UpdateRoom;
import com.hotel.room_service.dto.response.RoomByHotel;
import com.hotel.room_service.dto.response.Rooms;
import com.hotel.room_service.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getRooms(@RequestParam("hotelId") int hotelId) {
        Map<String, Object> response = new HashMap<>();
        try{
            List<RoomByHotel> roomByHotel = roomService.findRoomByHotelId(hotelId);
            if (roomByHotel != null) {
                response.put("rooms", roomByHotel);
                response.put("message", "Tìm thấy phòng cho khách sạn này");
            } else {
                response.put("message", "Không tìm thấy phòng cho khách sạn này");
            }
            return ResponseEntity.ok(response);
        }catch (Exception e){
            response.put("message", "Lỗi khi lấy phòng cho khách sạn này");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/rooms/available-count")
    public ResponseEntity<Map<String, Object>> getAvailableRoomCount(
            @RequestParam("hotelId") int hotelId,
            @RequestParam("checkInDate") String checkInDate,
            @RequestParam("checkOutDate") String checkOutDate,
            @RequestParam("guests") int guests,
            @RequestParam("rooms") int rooms) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Integer> result = roomService.getAvailableRoomCount(hotelId, checkInDate, checkOutDate, guests, rooms);
            response.putAll(result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("availableRooms", 0);
            response.put("totalRooms", 0);
            response.put("bookedRooms", 0);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/room/update" )
    public ResponseEntity<Map<String, Object>> updateRoom(@RequestBody UpdateRoom updateRoom) {
        Map<String, Object> response = new HashMap<>();
        try {
            Boolean updated = roomService.updateRoom(updateRoom);
            if (updated) {
                response.put("message", "Cập nhật phòng thành công");
            } else {
                response.put("message", "Cập nhật phòng thất bại");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Lỗi khi cập nhật phòng");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/room/add" )
    public ResponseEntity<Map<String, Object>> addRoom(@RequestBody AddRoom addRoom) {
        Map<String, Object> response = new HashMap<>();
        try {
            Boolean added = roomService.addRoom(addRoom);
            if (added) {
                response.put("message", "Thêm phòng thành công");
            } else {
                response.put("message", "Phòng đã tồn tại");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Lỗi khi thêm phòng");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/room/active" )
    public ResponseEntity<Map<String, Object>> activeRoom(@RequestBody UpdateActive updateActive) {
        Map<String, Object> response = new HashMap<>();
        try {
            Boolean deleted = roomService.activeRoom(updateActive);
            if (deleted) {
                response.put("message", "Cập nhật hoạt động phòng thành công");
            } else {
                response.put("message", "Cập nhật hoạt động phòng thất bại");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Lỗi khi xoá phòng");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/rooms/all" )
    public ResponseEntity<Map<String, Object>> getAllRooms(@RequestHeader("X-Auth-UserId") String userIdStr) {
        Map<String, Object> response = new HashMap<>();
        try {
            int userId = Integer.parseInt(userIdStr);
            List<Rooms> allRooms = roomService.getAllRooms(userId);
            if (allRooms != null && !allRooms.isEmpty()) {
                response.put("rooms", allRooms);
                response.put("message", "Lấy tất cả phòng thành công");
            } else {
                response.put("message", "Không có phòng nào");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Lỗi khi lấy tất cả phòng");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
