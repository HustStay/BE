package com.chat_service.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadStatusUpdate {
    private String type;
    private Integer readerType;
    private Long conversationId;
}
