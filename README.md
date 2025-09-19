# Email Handler

A scalable Spring Boot application that receives emails from IMAP servers, processes audio/video attachments by converting them to text using external APIs, and forwards the emails with transcripts to recipients.

## Features

- **IMAP Email Processing**: Connects to IMAP servers and fetches emails
- **Audio/Video Conversion**: Converts audio and video files to text using external APIs
- **Email Forwarding**: Forwards processed emails with transcripts while preserving headers
- **Duplicate Prevention**: Prevents processing the same email multiple times using database and Redis cache
- **Guaranteed Delivery**: Retry mechanism for failed processing
- **Scalable Processing**: Async processing with configurable thread pools
- **Configurable**: Extensive configuration options for all components

## Supported Formats

**Audio**: mp3, wav, m4a, aac, flac
**Video**: mp4, avi, mov, mkv, wmv

## Configuration

Configure the application using `application.yml`:

```yaml
email:
  handler:
    # IMAP Configuration
    imap:
      host: imap.gmail.com
      port: 993
      username: your-email@gmail.com
      password: your-password
      ssl-enabled: true
      folder: INBOX
      fetch-interval: 5000  # milliseconds
      max-messages-per-fetch: 1
    
    # SMTP Configuration for forwarding
    smtp:
      host: smtp.gmail.com
      port: 587
      username: forwarder@gmail.com
      password: forwarder-password
      starttls-enabled: true
      to-address: recipient@example.com
    
    # External API for audio/video conversion
    conversion-api:
      base-url: http://your-api-server.com
      endpoint: /api/audio-video/convert
      timeout: 300000  # 5 minutes
      retry-attempts: 3
      retry-delay: 5000
    
    # Processing Configuration
    processing:
      thread-pool-size: 10
      queue-capacity: 1000
      supported-audio-formats: mp3,wav,m4a,aac,flac
      supported-video-formats: mp4,avi,mov,mkv,wmv
      max-file-size: 100MB
    
    # Duplicate Prevention
    duplicate-prevention:
      enabled: true
      cache-duration: 24h
```

## Running the Application

1. **Prerequisites**:
   - Java 17 or later
   - Maven 3.6+
   - Redis server (optional, for caching)

2. **Build**:
   ```bash
   mvn clean package
   ```

3. **Run**:
   ```bash
   java -jar target/email-handler-1.0.0.jar
   ```

## Architecture

- **EmailProcessingService**: Main orchestrator that coordinates email processing
- **ImapEmailService**: Handles IMAP connection and email fetching
- **AudioVideoProcessingService**: Processes audio/video attachments
- **EmailForwardingService**: Forwards emails with preserved headers
- **DuplicatePreventionService**: Prevents duplicate processing using DB and Redis

## API Integration

The application expects the external conversion API to:
- Accept POST requests with multipart/form-data
- Process the uploaded file and return JSON with transcript
- Support the configured audio/video formats

Example API response:
```json
{
  "transcript": "This is the converted text from the audio/video file"
}
```

## Scalability Features

- **Async Processing**: Emails are processed asynchronously in thread pools
- **Configurable Concurrency**: Thread pool size and queue capacity are configurable
- **Rate Limiting**: Fetch interval controls how often emails are checked
- **Batch Processing**: Configurable number of emails fetched per cycle
- **Caching**: Redis caching for duplicate prevention

## Monitoring

The application includes:
- Comprehensive logging at various levels
- Spring Boot Actuator endpoints (when enabled)
- Database tracking of processed emails
- Error handling with retry mechanisms
