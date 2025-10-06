-- Seed data for The Experience LMS

-- Insert admin user
INSERT INTO users (id, email, name, image_url, role, created_at, updated_at) 
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'admin@thexperience.com',
    'Admin User',
    'https://via.placeholder.com/150/4F46E5/FFFFFF?text=A',
    'ADMIN',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Insert sample courses
INSERT INTO courses (id, title, description, thumbnail_url, is_published, created_at, updated_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440001',
    'Full-Stack Web Development',
    'Master modern web development with React, Node.js, and PostgreSQL. Build real-world applications from scratch.',
    '/fullstack-development.png',
    true,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440002',
    'UI/UX Design Fundamentals',
    'Learn the principles of user interface and user experience design. Create beautiful, functional designs.',
    '/ui-ux-design-concept.png',
    true,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440003',
    'React & JavaScript Mastery',
    'Deep dive into React hooks, state management, and modern JavaScript features.',
    '/react-javascript-code.png',
    true,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440004',
    'Database Design & SQL',
    'Learn database design principles, SQL queries, and optimization techniques.',
    '/database-sql.jpg',
    true,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440005',
    'REST API Development',
    'Build robust REST APIs with Spring Boot, authentication, and best practices.',
    '/api-rest-development.jpg',
    true,
    NOW(),
    NOW()
);

-- Insert modules for Full-Stack Web Development course
INSERT INTO modules (id, course_id, title, description, video_url, duration_seconds, order_index, created_at, updated_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440010',
    '550e8400-e29b-41d4-a716-446655440001',
    'Introduction to Modern Web Development',
    'Overview of the modern web development stack and tools you''ll be using.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    1800, -- 30 minutes
    1,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440011',
    '550e8400-e29b-41d4-a716-446655440001',
    'Setting Up Your Development Environment',
    'Install and configure all the necessary tools for full-stack development.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
    2400, -- 40 minutes
    2,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440012',
    '550e8400-e29b-41d4-a716-446655440001',
    'Building Your First React Application',
    'Create a complete React application with components, state, and routing.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    3600, -- 60 minutes
    3,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440013',
    '550e8400-e29b-41d4-a716-446655440001',
    'Backend Development with Node.js',
    'Build a RESTful API using Node.js, Express, and MongoDB.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    4200, -- 70 minutes
    4,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440014',
    '550e8400-e29b-41d4-a716-446655440001',
    'Database Integration and Deployment',
    'Connect your application to a database and deploy it to production.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    3000, -- 50 minutes
    5,
    NOW(),
    NOW()
);

-- Insert modules for UI/UX Design Fundamentals course
INSERT INTO modules (id, course_id, title, description, video_url, duration_seconds, order_index, created_at, updated_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440020',
    '550e8400-e29b-41d4-a716-446655440002',
    'Design Principles and Theory',
    'Learn the fundamental principles of good design and user experience.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',
    2700, -- 45 minutes
    1,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440021',
    '550e8400-e29b-41d4-a716-446655440002',
    'Wireframing and Prototyping',
    'Create wireframes and interactive prototypes for your designs.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4',
    3300, -- 55 minutes
    2,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440022',
    '550e8400-e29b-41d4-a716-446655440002',
    'User Research and Testing',
    'Conduct user research and usability testing to improve your designs.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
    2400, -- 40 minutes
    3,
    NOW(),
    NOW()
);

-- Insert modules for React & JavaScript Mastery course
INSERT INTO modules (id, course_id, title, description, video_url, duration_seconds, order_index, created_at, updated_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440030',
    '550e8400-e29b-41d4-a716-446655440003',
    'Advanced JavaScript Concepts',
    'Master closures, promises, async/await, and modern ES6+ features.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
    3600, -- 60 minutes
    1,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440031',
    '550e8400-e29b-41d4-a716-446655440003',
    'React Hooks Deep Dive',
    'Learn useState, useEffect, useContext, and custom hooks.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4',
    4200, -- 70 minutes
    2,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440032',
    '550e8400-e29b-41d4-a716-446655440003',
    'State Management with Redux',
    'Implement Redux for complex state management in React applications.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4',
    4800, -- 80 minutes
    3,
    NOW(),
    NOW()
);

-- Insert modules for Database Design & SQL course
INSERT INTO modules (id, course_id, title, description, video_url, duration_seconds, order_index, created_at, updated_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440040',
    '550e8400-e29b-41d4-a716-446655440004',
    'Database Design Fundamentals',
    'Learn normalization, relationships, and database design principles.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    3000, -- 50 minutes
    1,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440041',
    '550e8400-e29b-41d4-a716-446655440004',
    'Advanced SQL Queries',
    'Master complex SQL queries, joins, subqueries, and optimization.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
    3600, -- 60 minutes
    2,
    NOW(),
    NOW()
);

-- Insert modules for REST API Development course
INSERT INTO modules (id, course_id, title, description, video_url, duration_seconds, order_index, created_at, updated_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440050',
    '550e8400-e29b-41d4-a716-446655440005',
    'Spring Boot Fundamentals',
    'Get started with Spring Boot and create your first REST API.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    2700, -- 45 minutes
    1,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440051',
    '550e8400-e29b-41d4-a716-446655440005',
    'Authentication and Security',
    'Implement JWT authentication and secure your API endpoints.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    3300, -- 55 minutes
    2,
    NOW(),
    NOW()
),
(
    '550e8400-e29b-41d4-a716-446655440052',
    '550e8400-e29b-41d4-a716-446655440005',
    'Testing and Deployment',
    'Write comprehensive tests and deploy your API to production.',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    2400, -- 40 minutes
    3,
    NOW(),
    NOW()
);
