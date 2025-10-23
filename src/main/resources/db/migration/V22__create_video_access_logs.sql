-- Create video_access_logs table for Vimeo integration
CREATE TABLE video_access_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_ip VARCHAR(45),
    signed_url TEXT,
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    terms_accepted BOOLEAN NOT NULL DEFAULT false,
    
    CONSTRAINT fk_video_access_logs_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_video_access_logs_video_id ON video_access_logs(video_id);
CREATE INDEX idx_video_access_logs_user_id ON video_access_logs(user_id);
CREATE INDEX idx_video_access_logs_accessed_at ON video_access_logs(accessed_at);
CREATE INDEX idx_video_access_logs_expires_at ON video_access_logs(expires_at);

