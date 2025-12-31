# Batch Job - Google Drive File Uploader

Spring Boot web application for batch uploading files to Google Drive with PostgreSQL persistence.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Thymeleaf (web UI)
- PostgreSQL / H2 (persistence)
- Google Drive API v3

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL (or use H2 for local testing)
- Google Cloud project with Drive API enabled

### Build

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean package
```

### Run Locally

```bash
mvn spring-boot:run
```

The application starts at `http://localhost:8080`.

### Docker

```bash
docker build -t batch-job .
docker run -p 8080:8080 \
  -e GOOGLE_CLIENT_ID=your_client_id \
  -e GOOGLE_CLIENT_SECRET=your_secret \
  -e GOOGLE_REFRESH_TOKEN=your_token \
  -e GOOGLE_DRIVE_FOLDER_ID=your_folder_id \
  batch-job
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PGHOST` | PostgreSQL host | `localhost` |
| `PGPORT` | PostgreSQL port | `5432` |
| `PGDATABASE` | Database name | `batchjob` |
| `PGUSER` | Database user | `postgres` |
| `PGPASSWORD` | Database password | `postgres` |
| `GOOGLE_CLIENT_ID` | OAuth client ID | - |
| `GOOGLE_CLIENT_SECRET` | OAuth client secret | - |
| `GOOGLE_REFRESH_TOKEN` | OAuth refresh token | - |
| `GOOGLE_DRIVE_FOLDER_ID` | Target Drive folder | - |
| `PORT` | Server port | `8080` |

### File Size Limits

- Max file size: 50MB
- Max request size: 200MB

## Project Structure

```
src/main/java/com/batchjob/
├── BatchJobApplication.java      # Application entry point
├── config/
│   └── GoogleDriveConfig.java    # Google Drive client setup
├── controller/
│   └── FileUploadController.java # Web endpoints
├── entity/
│   └── FileRecord.java           # JPA entity
├── repository/
│   └── FileRecordRepository.java # Data access
└── service/
    ├── FileRecordService.java    # Business logic
    └── GoogleDriveService.java   # Drive API wrapper
```

## License

MIT
