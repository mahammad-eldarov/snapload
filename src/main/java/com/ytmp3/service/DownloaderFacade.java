package com.ytmp3.service;

import com.ytmp3.config.DownloaderConfig;
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

    public Map<String, Object> convert(String url) {

        String fileName = downloaderService.download(url);

        return Map.of(
                "status", "success",
                "message", "MP3 created successfully",
                "fileName", fileName,
                "outputDir", config.getOutputDir()
        );
    }

    public ResponseEntity<Resource> download(String fileName) {
        return fileService.download(fileName);
    }

    public ResponseEntity<Map<String, Object>> health() {

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "outputDir", config.getOutputDir(),
                "format", config.getAudioFormat()
        ));
    }
}
