package com.thexperience.backend.chat.dto;

import java.util.List;

public class ConversationHistoryResponse {
    private List<MessageDTO> messages;
    
    public ConversationHistoryResponse(List<MessageDTO> messages) {
        this.messages = messages;
    }
    
    public List<MessageDTO> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageDTO> messages) {
        this.messages = messages;
    }
}




