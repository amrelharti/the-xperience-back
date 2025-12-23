# Email Case-Sensitivity Fix

## Problem Solved

Users with emails stored in mixed case could not sign in because Google returns emails in lowercase, causing database lookup failures.

## Changes Applied

### 1. AuthController - Normalize emails during authentication
- Google OAuth: Email normalized to lowercase before database lookup
- Email/Password Auth: Email normalized to lowercase
- Error responses: All Map.of() replaced with HashMap to handle null values

### 2. ProfileController - Normalize emails during profile updates
- Email changes normalized to lowercase

### 3. MeController - Normalize emails during profile updates
- Email changes normalized to lowercase

### 4. Database Migration V54
- Converts all existing emails to lowercase
- Adds database constraint to enforce lowercase emails
- Creates case-insensitive index for better performance

## How to Apply

Restart the backend - migration will run automatically:

```bash
cd D:\Ali projet\the-xperience-back
mvn spring-boot:run
```

## Benefits

- Case-insensitive authentication
- Google OAuth compatibility
- Database integrity with constraint
- Better performance with index
- Null-safe responses




