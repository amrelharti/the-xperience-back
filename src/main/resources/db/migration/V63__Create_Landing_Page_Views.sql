-- ============================================
-- Landing Page Views Table
-- Version 63: Store landing page view tracking data
-- ============================================

CREATE TABLE IF NOT EXISTS landing_page_view (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    referer VARCHAR(500),
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create indexes for faster queries
CREATE INDEX idx_landing_page_view_viewed_at ON landing_page_view(viewed_at);
CREATE INDEX idx_landing_page_view_ip_address ON landing_page_view(ip_address);

-- Add comment
COMMENT ON TABLE landing_page_view IS 'Stores landing page view tracking data for analytics';

