package com.ytmp3.controller;

import com.ytmp3.dto.DownloadRequest;
import com.ytmp3.service.DownloaderFacade;
import com.ytmp3.service.DownloaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DownloaderController {

    private final DownloaderService downloaderService;
    private final DownloaderFacade downloaderFacade;

    @PostMapping("/stream")
    public ResponseBodyEmitter stream(@RequestBody DownloadRequest req) {
        return downloaderService.streamDownload(req);
    }

    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(@RequestBody DownloadRequest req) {
        return ResponseEntity.ok(downloaderFacade.convert(req));
    }

    @PostMapping("/download")
    public ResponseEntity<String> download(@RequestBody DownloadRequest req) {
        String fileName = downloaderService.download(req);
        return ResponseEntity.ok(fileName);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return downloaderFacade.health();
    }
}