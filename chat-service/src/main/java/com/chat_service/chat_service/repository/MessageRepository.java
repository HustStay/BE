package com.chat_service.chat_service.repository;

import com.chat_service.chat_service.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.senderType = :senderType AND m.isRead = false")
    Integer countUnreadMessages(@Param("conversationId") Long conversationId, @Param("senderType") Integer senderType);
    
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.senderType = :senderType AND m.isRead = false")
    void markMessagesAsRead(@Param("conversationId") Long conversationId, @Param("senderType") Integer senderType);
    
    Message findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
