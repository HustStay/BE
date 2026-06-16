package com.chat_service.chat_service.controller;

import com.chat_service.chat_service.dto.ChatMessageDTO;
import com.chat_service.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/message")
    public void handleChatMessage(@Payload ChatMessageDTO message) {
        log.info("Received WebSocket message: conversationId={}, senderId={}, type={}", 
                message.getConversationId(), message.getSenderId(), message.getMessageType());
        
        ChatMessageDTO savedMessage = chatService.saveMessage(message);
        
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getConversationId(), 
                savedMessage
        );
        
        log.info("Broadcasted message to /topic/chat/{}", message.getConversationId());
    }
}
