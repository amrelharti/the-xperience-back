-- Add some sample draft modules for testing
INSERT INTO modules (id, title, description, thumbnail_url, is_published, created_at, updated_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440100',
    'Advanced React Patterns',
    'Master advanced React patterns including render props, higher-order components, and custom hooks.',
    'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=400&h=225&fit=crop',
    false,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440101',
    'Machine Learning Fundamentals',
    'Introduction to machine learning concepts, algorithms, and practical applications.',
    'https://images.unsplash.com/photo-1555949963-aa79dcee981c?w=400&h=225&fit=crop',
    false,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440102',
    'DevOps & CI/CD Pipeline',
    'Learn to build robust CI/CD pipelines and deploy applications with Docker and Kubernetes.',
    'https://images.unsplash.com/photo-1667372393120-14153c5e8a3b?w=400&h=225&fit=crop',
    false,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440103',
    'Blockchain Development',
    'Build decentralized applications using Solidity and Web3 technologies.',
    'https://images.unsplash.com/photo-1639762681485-074b7f938ba0?w=400&h=225&fit=crop',
    false,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440104',
    'Mobile App Development',
    'Create cross-platform mobile applications using React Native and Flutter.',
    'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=400&h=225&fit=crop',
    false,
    NOW(),
    NOW()
);
