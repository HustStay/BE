package com.chat_service.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long id;
    private Long customerId;
    private Long hotelId;
    private String customerName;
    private String lastMessage;
    private String lastMessageTime;
    private Integer unreadCount;
    private String createdAt;
}
