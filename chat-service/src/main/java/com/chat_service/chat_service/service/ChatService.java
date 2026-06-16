package com.chat_service.chat_service.service;

import com.chat_service.chat_service.dto.ChatMessageDTO;
import com.chat_service.chat_service.model.Message;
import com.chat_service.chat_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Transactional
    public ChatMessageDTO saveMessage(ChatMessageDTO messageDTO) {
        Message message = Message.builder()
                .conversationId(messageDTO.getConversationId())
                .senderId(messageDTO.getSenderId())
                .senderType(messageDTO.getSenderType())
                .content(messageDTO.getContent())
                .messageType(messageDTO.getMessageType())
                .isRead(false)
                .fileName(messageDTO.getFileName())
                .fileSize(messageDTO.getFileSize())
                .fileType(messageDTO.getFileType())
                .fileUrl(messageDTO.getFileUrl())
                .thumbnailUrl(messageDTO.getThumbnailUrl())
                .build();
        
        Message savedMessage = messageRepository.save(message);
        log.info("Message saved: id={}, conversationId={}, type={}", 
                savedMessage.getId(), savedMessage.getConversationId(), savedMessage.getMessageType());
        
        return convertToDTO(savedMessage);
    }

    public List<ChatMessageDTO> getMessages(Long conversationId) {
        log.info("Getting messages for conversationId={}", conversationId);
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        log.info("Found {} messages for conversationId={}", messages.size(), conversationId);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public int markMessagesAsRead(Long conversationId, Integer senderType) {
        int count = messageRepository.countUnreadMessages(conversationId, senderType);
        messageRepository.markMessagesAsRead(conversationId, senderType);
        log.info("Marked {} messages as read: conversationId={}, senderType={}", count, conversationId, senderType);
        return count;
    }

    public Integer getUnreadCount(Long conversationId, Integer senderType) {
        return messageRepository.countUnreadMessages(conversationId, senderType);
    }

    private ChatMessageDTO convertToDTO(Message message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().format(formatter) : null)
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .fileType(message.getFileType())
                .fileUrl(message.getFileUrl())
                .thumbnailUrl(message.getThumbnailUrl())
                .build();
    }
}
