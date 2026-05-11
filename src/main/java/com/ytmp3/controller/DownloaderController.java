package com.ytmp3.controller;

import com.ytmp3.service.DownloaderFacade;
import com.ytmp3.service.DownloaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseBodyEmitter stream(@RequestParam String url) {
        return downloaderService.streamDownload(url);
    }
    
    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(downloaderFacade.convert(body.get("url")));
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        return downloaderFacade.download(fileName);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return downloaderFacade.health();
    }
}