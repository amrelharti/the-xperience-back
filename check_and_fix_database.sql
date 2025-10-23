-- Check and Fix Database Schema
-- This script checks the current state and fixes it

-- First, let's see what we have
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trainings') 
        THEN 'trainings table EXISTS' 
        ELSE 'trainings table DOES NOT EXIST' 
    END as trainings_status,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'modules') 
        THEN 'modules table EXISTS' 
        ELSE 'modules table DOES NOT EXIST' 
    END as modules_status,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'classes' AND column_name = 'training_id'
        ) 
        THEN 'classes.training_id EXISTS' 
        ELSE 'classes.training_id DOES NOT EXIST' 
    END as classes_training_id_status,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'classes' AND column_name = 'module_id'
        ) 
        THEN 'classes.module_id EXISTS' 
        ELSE 'classes.module_id DOES NOT EXIST' 
    END as classes_module_id_status;

-- Show Flyway version
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank DESC 
LIMIT 5;



