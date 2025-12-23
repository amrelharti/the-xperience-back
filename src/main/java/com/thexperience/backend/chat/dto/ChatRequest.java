package com.thexperience.backend.chat.dto;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    private String message;
    private List<Map<String, String>> conversationHistory;
    private String cvContext;
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<Map<String, String>> getConversationHistory() {
        return conversationHistory;
    }
    
    public void setConversationHistory(List<Map<String, String>> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
    
    public String getCvContext() {
        return cvContext;
    }
    
    public void setCvContext(String cvContext) {
        this.cvContext = cvContext;
    }
}

