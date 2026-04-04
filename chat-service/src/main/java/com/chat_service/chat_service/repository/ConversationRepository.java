package com.chat_service.chat_service.repository;


import com.chat_service.chat_service.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByCustomerIdAndHotelId(Long customerId, Long hotelId);
    List<Conversation> findByHotelId(Long hotelId);
    List<Conversation> findByCustomerId(Long customerId);
}
