# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Run locally
mvn spring-boot:run

# Run tests
mvn test

# Run single test class
mvn test -Dtest=TestClassName

# Docker build and run
docker build -t batch-job .
docker run -p 8080:8080 batch-job
```

## Required Environment Variables

- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` - PostgreSQL connection (defaults to localhost:5432/batchjob)
- `GOOGLE_CREDENTIALS` - Google service account JSON credentials (as string)
- `GOOGLE_DRIVE_FOLDER_ID` - Target Google Drive folder ID for uploads
- `PORT` - Server port (default: 8080)

## Architecture Overview

Spring Boot 3.2 web application for batch file uploads to Google Drive with PostgreSQL persistence.

**Request Flow:**
1. `FileUploadController` handles web requests (Thymeleaf MVC)
2. `FileRecordService` orchestrates upload + persistence
3. `GoogleDriveService` uploads to Google Drive API
4. `FileRecord` entity persisted via JPA repository

**Key Components:**
- `GoogleDriveConfig` - Creates Drive client bean from `GOOGLE_CREDENTIALS` env var using service account auth
- `GoogleDriveService` - Uses `setSupportsAllDrives(true)` for shared drive compatibility
- File size limits: 50MB per file, 200MB per request (configured in application.yml)

**Database:**
Single `file_records` table with auto-DDL update. Schema managed by Hibernate.
