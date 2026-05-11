# YouTube → MP3 Converter (Spring Boot)

A Spring Boot based YouTube to MP3 converter using yt-dlp and ffmpeg with real-time streaming support via Server-Sent Events (SSE).

---

## Features

- Convert YouTube videos to MP3
- Real-time download/conversion progress
- Automatic metadata embedding
- Thumbnail embedding
- Async temporary file cleanup
- Dockerized setup
- REST API support
- PowerShell CLI integration

---

## Requirements
- Java 17
- ffmpeg: https://ffmpeg.org/download.html //ffmpeg.exe and ffprobe.exe - Paste these .exe files into the C:\Windows\System32 folder.
- yt-dlp: https://github.com/yt-dlp/yt-dlp/releases //yt-dlp.exe - Paste this .exe files into the C:\Windows\System32 folder.

## Build & Run

```bash
./gradlew bootRun
```

## Usage

### First Time Setup

Open PowerShell, navigate to the project folder and run:

```powershell
cd path\to\ytmp3
.\yt "https://youtu.be/VIDEO_ID"
```

This will automatically add the project to PATH. Reopen the terminal.

### After Setup

Make sure the Spring Boot application is running, then from anywhere:

```powershell
yt "https://youtu.be/VIDEO_ID"
```

The MP3 file will be saved in the `downloads` folder inside the project directory.

## API

```bash

# Conversion using Terminal
curl -X POST http://localhost:8080/api/convert \
  -H "Content-Type: application/json" \
  -d '{"url": "youtube video link"}'
  
# If you are using Windows OS, you can use the following commands in PowerShell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/convert" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"url":"youtube video link"}'  
  
# Conversion using Postman
http://localhost:8080/api/convert //body {"url": "youtube video link"}

# Download file
curl -O http://localhost:8080/api/download/Song.mp3

# Health
curl http://localhost:8080/api/health
```

## Structure

```
src/main/
├── java/com/ytmp3/
│   ├── Ytmp3Application.java
│   │
│   ├── config/
│   │   └── DownloaderConfig.java
│   │
│   ├── controller/
│   │   └── DownloaderController.java
│   │
│   ├── service/
│   │   ├── DownloaderFacade.java
│   │   ├── DownloaderService.java
│   │   └── FileService.java
│   │
│   └── exception/
│       ├── DownloadException.java
│       └── GlobalExceptionHandler.java
│
└── resources/
    └── application.yaml
```
## Docker Support

The application is containerized using Docker for easier deployment and environment consistency.

### Build

#Build image
```bash
docker build -t ytmp3 .
```
#Run project
```bash
docker run -p 8080:8080 ytmp3
```

## Notice
1. After the video is converted, the mp3 file will be saved to the download folder on your computer.
2. If there is a problem saving to the download folder on your computer, if you convert the video to mp3 format for the first time, a .downloads folder will be created in the project folder and saved there. It will not create a new folder next time.