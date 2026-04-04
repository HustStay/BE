package com.chat_service.chat_service.controller;

import com.chat_service.chat_service.dto.ChatMessageDTO;
import com.chat_service.chat_service.dto.ConversationDTO;
import com.chat_service.chat_service.dto.ReadStatusUpdate;
import com.chat_service.chat_service.dto.StartChatRequest;
import com.chat_service.chat_service.model.Conversation;
import com.chat_service.chat_service.service.ChatService;
import com.chat_service.chat_service.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {
    
    private final ChatService chatService;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Start or get existing conversation
     * FE Customer calls: POST /api/chat-service/chat/start with body {hotelId}
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startConversation(
            @RequestBody StartChatRequest request,
            @RequestHeader(value = "X-Auth-UserId", required = false) String userIdStr) {
        
        // Get customerId from header (injected by API Gateway) or from request body
        Long customerId = request.getCustomerId();
        if (userIdStr != null && !userIdStr.isEmpty()) {
            customerId = Long.parseLong(userIdStr);
        }
        
        log.info("Starting conversation: customerId={}, hotelId={}", customerId, request.getHotelId());
        
        Conversation conversation = conversationService.startConversation(customerId, request.getHotelId());
        
        return ResponseEntity.ok(Map.of(
            "message", "Chat started successfully",
            "data", Map.of(
                "id", conversation.getId(),
                "customerId", conversation.getCustomerId(),
                "hotelId", conversation.getHotelId(),
                "createdAt", conversation.getCreatedAt()
            )
        ));
    }

    /**
     * Get messages for a conversation
     * FE calls: GET /api/chat-service/chat/messages?conversationId={}&hotelId={}
     */
    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @RequestParam Long conversationId,
            @RequestParam(required = false) Long hotelId,
            @RequestHeader(value = "X-Auth-UserId", required = false) String userIdStr) {
        
        log.info("Fetching messages: conversationId={}, hotelId={}, userId={}", conversationId, hotelId, userIdStr);
        
        List<ChatMessageDTO> messages = chatService.getMessages(conversationId);
        
        return ResponseEntity.ok(Map.of("data", messages));
    }

    /**
     * Mark messages as read
     * FE calls: PUT /api/chat-service/chat/messages/mark-read?conversationId={}&readerType={}
     * readerType: 0 = customer reading hotel messages, 1 = hotel reading customer messages
     */
    @PutMapping("/messages/mark-read")
    public ResponseEntity<Map<String, Object>> markMessagesAsRead(
            @RequestParam Long conversationId,
            @RequestParam Integer readerType,
            @RequestHeader(value = "X-Auth-UserId", required = false) String userIdStr) {
        
        log.info("Marking messages as read: conversationId={}, readerType={}, userId={}", 
                conversationId, readerType, userIdStr);
        
        // senderType is opposite of readerType
        // If customer (readerType=0) is reading, mark hotel messages (senderType=1) as read
        // If hotel (readerType=1) is reading, mark customer messages (senderType=0) as read
        Integer senderType = readerType == 0 ? 1 : 0;
        int markedCount = chatService.markMessagesAsRead(conversationId, senderType);
        
        // Broadcast read status update via WebSocket
        ReadStatusUpdate statusUpdate = ReadStatusUpdate.builder()
                .type("READ_STATUS_UPDATE")
                .readerType(readerType)
                .conversationId(conversationId)
                .build();
        
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, statusUpdate);
        log.info("Broadcasted read status update to /topic/chat/{}", conversationId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Messages marked as read successfully",
            "markedCount", markedCount
        ));
    }

    /**
     * Get conversations for hotel
     * FE Hotel calls: GET /api/chat-service/chat/conversations
     * Uses X-Auth-UserId header to get hotelId
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(
            @RequestHeader(value = "X-Auth-UserId", required = false) String userIdStr,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) Long customerId) {
        
        log.info("Fetching conversations: userIdStr={}, hotelId={}, customerId={}", userIdStr, hotelId, customerId);
        
        List<ConversationDTO> conversations;
        
        // If hotelId is provided in param, use it
        if (hotelId != null) {
            conversations = conversationService.getConversationsByHotelId(hotelId);
        } 
        // If customerId is provided in param, use it  
        else if (customerId != null) {
            conversations = conversationService.getConversationsByCustomerId(customerId);
        }
        // Otherwise use the userId from header (hotel user)
        else if (userIdStr != null && !userIdStr.isEmpty()) {
            Long userId = Long.parseLong(userIdStr);
            conversations = conversationService.getConversationsByHotelId(userId);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "hotelId or customerId required"));
        }
        
        return ResponseEntity.ok(Map.of("data", conversations));
    }
}
