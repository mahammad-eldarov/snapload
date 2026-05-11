# YouTube → MP3 / MP4 Converter (Spring Boot)

A Spring Boot based YouTube to MP3 and MP4 converter using yt-dlp and ffmpeg with real-time streaming support via Server-Sent Events (SSE).

---

## Features

- Convert YouTube videos to MP3 or MP4
- Trim video by specifying start and end time (MP4 only)
- Real-time download/conversion progress
- Automatic metadata and thumbnail embedding (MP3)
- Parallel fragment downloading for faster conversions
- Async temporary file cleanup
- Dockerized setup
- REST API support
- PowerShell CLI integration

---

## Requirements

- Java 17
- ffmpeg: https://ffmpeg.org/download.html — paste `ffmpeg.exe` and `ffprobe.exe` into `C:\Windows\System32`
- yt-dlp: https://github.com/yt-dlp/yt-dlp/releases — paste `yt-dlp.exe` into `C:\Windows\System32`

> **MP4 playback on Windows:** Install the [AV1 Video Extension](https://apps.microsoft.com/detail/9MVZQVXJBQ9V) from the Microsoft Store for full codec support. Alternatively, use [VLC Media Player](https://www.videolan.org/).

---

## Build & Run

```bash
./gradlew bootRun
```

---

## Usage

### First Time Setup

Open PowerShell, navigate to the project folder and run:

```powershell
cd path\to\project
.\yt "https://youtu.be/VIDEO_ID"
```

This will automatically add the project to PATH. Reopen the terminal after setup.

### After Setup

Make sure the Spring Boot application is running, then from anywhere:

```powershell
# Download as MP3 (default)
yt "https://youtu.be/VIDEO_ID"
yt "https://youtu.be/VIDEO_ID" mp3

# Download as MP4 (full video)
yt "https://youtu.be/VIDEO_ID" mp4

# Download MP4 trimmed (start second → end second)
yt "https://youtu.be/VIDEO_ID" mp4 30 90
```

> Files are saved to the `downloads` folder inside the project directory.

---

## API

### Convert (non-streaming)

```bash
# Terminal
curl -X POST http://localhost:8080/api/convert \
  -H "Content-Type: application/json" \
  -d '{"url": "VIDEO_URL", "format": "mp3"}'

# PowerShell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/convert" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"url": "VIDEO_URL", "format": "mp3"}'
```

### Stream (real-time progress)

```bash
curl -X POST http://localhost:8080/api/stream \
  -H "Content-Type: application/json" \
  -d '{"url": "VIDEO_URL", "format": "mp4", "startSec": 30, "endSec": 90}'
```

### Download file

```bash
curl -O http://localhost:8080/api/download/filename.mp3
```

### Health check

```bash
curl http://localhost:8080/api/health
```

### Request body reference

| Field | Type | Required | Description |
|---|---|---|---|
| url | String | Yes | YouTube video URL |
| format | String | Yes | `mp3` or `mp4` |
| startSec | Integer | No | Trim start (seconds) — MP4 only |
| endSec | Integer | No | Trim end (seconds) — MP4 only |

> `startSec` and `endSec` must be provided together. `endSec` must be greater than `startSec`.

---

## Configuration

`application.yaml` / `application.properties`:

```properties
downloader.output-dir=./downloads
downloader.audio-format=mp3
downloader.audio-quality=0
downloader.embed-thumbnail=true
downloader.add-metadata=true
downloader.filename-template=%(title)s.%(ext)s
downloader.yt-dlp-command=yt-dlp
downloader.ffmpeg-command=ffmpeg
downloader.video-quality=1080
downloader.concurrent-fragments=4
```

---

## Structure

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
│   ├── dto/
│   │   └── DownloadRequest.java
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

---

## Docker Support

```bash
# Build image
docker build -t ytmp3 .

# Run project
docker run -p 8080:8080 ytmp3
```

---

## Notes

1. Converted files are saved to the `downloads` folder in the project directory.
2. If the `downloads` folder does not exist, it is created automatically on first use.
3. Playlist URLs are supported — only the specified video is downloaded, not the full playlist.
4. MP4 trim is based on seconds. Example: `startSec=30, endSec=90` extracts the 30–90 second range.
5. MP4 video quality defaults to 1080p. If the video does not have 1080p, the best available quality is used.