-- ============================================
-- Chat Conversation History Table
-- Version 56: CV Scorer Chatbot
-- ============================================

CREATE TABLE IF NOT EXISTS chat_conversation_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'user' or 'assistant'
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for faster queries
CREATE INDEX idx_chat_message_user_id ON chat_conversation_message(user_id);
CREATE INDEX idx_chat_message_created_at ON chat_conversation_message(created_at);

-- Add comment
COMMENT ON TABLE chat_conversation_message IS 'Stores conversation history for CV scorer chatbot per user';




