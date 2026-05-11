package com.ytmp3.service;

import com.ytmp3.config.DownloaderConfig;
import com.ytmp3.dto.DownloadRequest;
import org.springframework.core.io.Resource;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DownloaderFacade {

    private final DownloaderService downloaderService;
    private final FileService fileService;
    private final DownloaderConfig config;

    public Map<String, Object> convert(DownloadRequest req) {

        String fileName = downloaderService.download(req);

        String message = req.getFormat().equalsIgnoreCase("mp4")
                ? "MP4 created successfully"
                : "MP3 created successfully";

        return Map.of(
                "status",     "success",
                "message",    message,
                "fileName",   fileName,
                "outputDir",  config.getOutputDir(),
                "format",     req.getFormat()
        );
    }

    public ResponseEntity<Resource> download(String fileName) {
        return fileService.download(fileName);
    }

    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status",      "UP",
                "outputDir",   config.getOutputDir(),
                "audioFormat", config.getAudioFormat(),
                "videoFormat", config.getVideoFormat()
        ));
    }
}