-- ============================================
-- Landing Page Settings Table
-- Version 59: Store landing page settings like remaining spots
-- ============================================

CREATE TABLE IF NOT EXISTS landing_page_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    remaining_spots INTEGER NOT NULL DEFAULT 12,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default settings (only if table is empty)
INSERT INTO landing_page_settings (remaining_spots)
SELECT 12
WHERE NOT EXISTS (SELECT 1 FROM landing_page_settings);

-- Create index
CREATE INDEX idx_landing_page_settings_updated_at ON landing_page_settings(updated_at);

-- Add comment
COMMENT ON TABLE landing_page_settings IS 'Stores landing page settings like remaining spots counter';

