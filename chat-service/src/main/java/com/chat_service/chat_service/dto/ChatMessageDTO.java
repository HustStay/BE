package com.chat_service.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Integer senderType;
    private String content;
    private String messageType;
    private Boolean isRead;
    private String createdAt;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileUrl;
    private String thumbnailUrl;
    private String customerName;
    private String hotelName;
}
