-- Create posts table (replaces homeworks with more social media features)
CREATE TABLE IF NOT EXISTS posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id UUID REFERENCES modules(id) ON DELETE SET NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'POST' CHECK (type IN ('POST', 'ASSIGNMENT', 'QUESTION', 'ANNOUNCEMENT')),
    title VARCHAR(255),
    content TEXT NOT NULL,
    attachments TEXT,
    is_assignment BOOLEAN DEFAULT false,
    assignment_module_id UUID REFERENCES modules(id) ON DELETE SET NULL,
    avg_score DECIMAL(3,2) DEFAULT 0.0,
    score_count INTEGER DEFAULT 0,
    is_hidden BOOLEAN DEFAULT false,
    is_pinned BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create comments table
CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    parent_comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    is_hidden BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create post_likes table for engagement
CREATE TABLE IF NOT EXISTS post_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

-- Create comment_likes table
CREATE TABLE IF NOT EXISTS comment_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(comment_id, user_id)
);

-- Migrate existing homeworks to posts
INSERT INTO posts (id, author_id, module_id, type, title, content, attachments, is_assignment, assignment_module_id, avg_score, score_count, is_hidden, created_at, updated_at)
SELECT 
    id,
    author_id,
    module_id,
    'ASSIGNMENT',
    title,
    body,
    attachments,
    true,
    module_id,
    avg_score,
    score_count,
    is_hidden,
    created_at,
    updated_at
FROM homeworks;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts(author_id);
CREATE INDEX IF NOT EXISTS idx_posts_module_id ON posts(module_id);
CREATE INDEX IF NOT EXISTS idx_posts_type ON posts(type);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at);
CREATE INDEX IF NOT EXISTS idx_posts_is_pinned ON posts(is_pinned);
CREATE INDEX IF NOT EXISTS idx_posts_is_assignment ON posts(is_assignment);

CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments(author_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_comment_id ON comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at);

CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_likes_user_id ON post_likes(user_id);

CREATE INDEX IF NOT EXISTS idx_comment_likes_comment_id ON comment_likes(comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_likes_user_id ON comment_likes(user_id);

-- Insert some sample posts
INSERT INTO posts (author_id, module_id, type, title, content, is_assignment, created_at)
SELECT 
    u.id,
    m.id,
    'POST',
    'Just finished ' || m.title || '!',
    'Really enjoyed learning about ' || m.title || '. The concepts were challenging but very rewarding. Looking forward to the next module!',
    false,
    CURRENT_TIMESTAMP - INTERVAL '2 hours'
FROM users u
CROSS JOIN modules m
WHERE u.role = 'STUDENT'
LIMIT 5;

-- Insert some sample questions
INSERT INTO posts (author_id, module_id, type, title, content, is_assignment, created_at)
SELECT 
    u.id,
    m.id,
    'QUESTION',
    'Question about ' || m.title,
    'I''m having trouble understanding the concept of ' || m.title || '. Can someone help explain it in simpler terms?',
    false,
    CURRENT_TIMESTAMP - INTERVAL '1 hour'
FROM users u
CROSS JOIN modules m
WHERE u.role = 'STUDENT'
LIMIT 3;
