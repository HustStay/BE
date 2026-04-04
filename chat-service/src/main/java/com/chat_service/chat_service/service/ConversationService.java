package com.chat_service.chat_service.service;

import com.chat_service.chat_service.client.HotelServiceClient;
import com.chat_service.chat_service.client.UserServiceClient;
import com.chat_service.chat_service.dto.ConversationDTO;
import com.chat_service.chat_service.model.Conversation;
import com.chat_service.chat_service.model.Message;
import com.chat_service.chat_service.repository.ConversationRepository;
import com.chat_service.chat_service.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserServiceClient userServiceClient;
    private final HotelServiceClient hotelServiceClient;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Start a conversation between customer and hotel owner
     * @param customerId - ID của khách hàng
     * @param hotelId - ID của khách sạn (từ bảng homes), cần chuyển thành ownerId
     */
    @Transactional
    public Conversation startConversation(Long customerId, Long hotelId) {
        // Get ownerId (userId of hotel owner) from hotelId (hotel's ID in homes table)
        Long ownerUserId = getOwnerIdFromHotelId(hotelId);
        
        log.info("Starting conversation: customerId={}, hotelId={}, ownerUserId={}", customerId, hotelId, ownerUserId);
        
        // Use ownerUserId as the hotelId in conversation (hotelId in conversation = userId of hotel owner)
        return conversationRepository.findByCustomerIdAndHotelId(customerId, ownerUserId)
                .orElseGet(() -> {
                    Conversation newConversation = Conversation.builder()
                            .customerId(customerId)
                            .hotelId(ownerUserId)  // Store owner's userId, not hotel's ID
                            .build();
                    Conversation saved = conversationRepository.save(newConversation);
                    log.info("Created new conversation: id={}, customerId={}, hotelOwnerId={}", 
                            saved.getId(), customerId, ownerUserId);
                    return saved;
                });
    }
    
    /**
     * Get owner's userId from hotel's ID
     */
    private Long getOwnerIdFromHotelId(Long hotelId) {
        try {
            Map<String, Object> response = hotelServiceClient.getOwnerId(hotelId.intValue());
            if (response != null && response.get("ownerId") != null) {
                return Long.valueOf(response.get("ownerId").toString());
            }
        } catch (Exception e) {
            log.error("Failed to get ownerId from hotelId={}: {}", hotelId, e.getMessage());
        }
        // Fallback: return hotelId as-is (in case it's already the owner's userId)
        return hotelId;
    }

    public List<ConversationDTO> getConversationsByHotelId(Long hotelId) {
        log.info("Getting conversations for hotelId={} (this is the userId of hotel owner)", hotelId);
        List<Conversation> conversations = conversationRepository.findByHotelId(hotelId);
        log.info("Found {} conversations for hotelId={}", conversations.size(), hotelId);
        return conversations.stream()
                .map(this::convertToDTO)
                .sorted((a, b) -> {
                    // Sort by lastMessageTime descending, null last
                    if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                    if (a.getLastMessageTime() == null) return 1;
                    if (b.getLastMessageTime() == null) return -1;
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                })
                .collect(Collectors.toList());
    }

    public List<ConversationDTO> getConversationsByCustomerId(Long customerId) {
        List<Conversation> conversations = conversationRepository.findByCustomerId(customerId);
        return conversations.stream()
                .map(this::convertToDTO)
                .sorted((a, b) -> {
                    if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                    if (a.getLastMessageTime() == null) return 1;
                    if (b.getLastMessageTime() == null) return -1;
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                })
                .collect(Collectors.toList());
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        Message lastMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId());
        // Count unread messages from customer (senderType = 0)
        Integer unreadCount = messageRepository.countUnreadMessages(conversation.getId(), 0);
        
        // Get customer name from user-service
        String customerName = "Khách hàng";
        try {
            Map<String, Object> response = userServiceClient.getFullName((int) conversation.getCustomerId());
            if (response != null && response.get("fullName") != null) {
                customerName = response.get("fullName").toString();
            }
        } catch (Exception e) {
            log.warn("Failed to get customer name for customerId={}: {}", conversation.getCustomerId(), e.getMessage());
        }
        
        return ConversationDTO.builder()
                .id(conversation.getId())
                .customerId(conversation.getCustomerId())
                .hotelId(conversation.getHotelId())
                .customerName(customerName)
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageTime(lastMessage != null && lastMessage.getCreatedAt() != null ? 
                        lastMessage.getCreatedAt().format(formatter) : null)
                .unreadCount(unreadCount)
                .createdAt(conversation.getCreatedAt() != null ? 
                        conversation.getCreatedAt().format(formatter) : null)
                .build();
    }
}
