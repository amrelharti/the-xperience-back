-- ============================================
-- Landing Page Leads Table
-- Version 58: Store leads from landing page form submissions
-- ============================================

CREATE TABLE IF NOT EXISTS landing_page_lead (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    looking_for VARCHAR(100) NOT NULL,
    offer VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster queries
CREATE INDEX idx_landing_page_lead_phone ON landing_page_lead(phone);
CREATE INDEX idx_landing_page_lead_created_at ON landing_page_lead(created_at);

-- Add comment
COMMENT ON TABLE landing_page_lead IS 'Stores leads from landing page form submissions';

