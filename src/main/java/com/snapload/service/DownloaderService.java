package com.snapload.service;

import com.snapload.config.DownloaderConfig;
import com.snapload.dto.DownloadRequest;
import com.snapload.exception.DownloadException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloaderService {

    private final DownloaderConfig config;

    public ResponseBodyEmitter streamDownload(DownloadRequest req) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                Process process = startProcess(req);
                streamProcess(process, line -> {
                    try {
                        emitter.send(line + "\n");
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });
                int exitCode = process.waitFor();
                CompletableFuture.runAsync(this::deleteTempFiles);
                if (exitCode == 0) emitter.send("STATUS:DONE\n");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    public String download(DownloadRequest req) {
        log.info("Download started: {} [{}]", req.getUrl(), req.getFormat());
        try {
            Process process = startProcess(req);
            StringBuilder output = new StringBuilder();
            streamProcess(process, line -> output.append(line).append("\n"));
            int exitCode = process.waitFor();
            if (exitCode != 0) throw new DownloadException("yt-dlp failed:\n" + output);
            String fileName = extractFileName(output.toString());
            log.info("Download completed: {}", fileName);
            CompletableFuture.runAsync(this::deleteTempFiles);
            return fileName;
        } catch (IOException e) {
            throw new DownloadException("Process error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DownloadException("Interrupted");
        }
    }

    public Process startProcess(DownloadRequest req) throws IOException {
        validateUrl(req.getUrl());
        validateRequest(req);
        checkDependencies();
        createOutputDir();
        return new ProcessBuilder(buildCommand(req))
                .redirectErrorStream(true)
                .start();
    }

    public void streamProcess(Process process, Consumer<String> consumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[yt-dlp] {}", line);
                consumer.accept(line);
            }
        }
    }

    public List<String> buildCommand(DownloadRequest req) {
        List<String> cmd = new ArrayList<>(List.of(config.getYtDlpCommand()));

        if ("mp4".equalsIgnoreCase(req.getFormat()) || "mkv".equalsIgnoreCase(req.getFormat())) {
            cmd.addAll(List.of(
                    "-f", "bestvideo[vcodec^=avc1][height<=" + config.getVideoQuality() + "]+bestaudio[ext=m4a]" +
                            "/bestvideo[height<=" + config.getVideoQuality() + "]+bestaudio[ext=m4a]",
                    "--merge-output-format", "mp4",
                    "--no-playlist"
            ));
            if (req.getStartSec() != null && req.getEndSec() != null) {
                cmd.addAll(List.of(
                        "--download-sections", "*" + req.getStartSec() + "-" + req.getEndSec()
                ));
            }
        } else {
            cmd.addAll(List.of(
                    "--extract-audio",
                    "--audio-format",  config.getAudioFormat(),
                    "--audio-quality", String.valueOf(config.getAudioQuality()),
                    "--no-playlist"
            ));

            if (config.isEmbedThumbnail()) cmd.add("--embed-thumbnail");
            if (config.isAddMetadata()) cmd.add("--add-metadata");
        }

        cmd.addAll(List.of("--output", buildOutputTemplate(req.getFormat()), req.getUrl()));
        return cmd;
    }

    public void deleteTempFiles() {
        Path tempPath = Paths.get("tmp");
        if (!Files.exists(tempPath)) return;
        try (Stream<Path> files = Files.walk(tempPath)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                    log.info("Deleted: {}", file.getFileName());
                } catch (IOException e) {
                    log.error("Delete failed: {}", file, e);
                }
            });
        } catch (Exception e) {
            log.error("Cleanup failed", e);
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("URL empty");
        if (!url.matches("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$"))
            throw new IllegalArgumentException("Invalid YouTube URL");
    }

    private void checkDependencies() {
        String tool = config.getYtDlpCommand();
        try {
            int exit = new ProcessBuilder(tool, "--version")
                    .redirectErrorStream(true).start().waitFor();
            if (exit != 0) throw new DownloadException(tool + " not working");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new DownloadException(tool + " not found");
        }
    }

    private String extractFileName(String output) {
        return Arrays.stream(output.split("\n"))
                .filter(l -> l.contains("Destination:"))
                .map(l -> l.substring(l.indexOf("Destination:") + 13).trim())
                .reduce((a, b) -> b)
                .orElse("output_" + System.currentTimeMillis() + ".mp3");
    }

    private void createOutputDir() {
        try {
            Files.createDirectories(Path.of(config.getOutputDir()));
        } catch (IOException e) {
            log.warn("Folder error: {}", e.getMessage());
        }
    }

    private String buildOutputTemplate(String format) {
        return Path.of(config.getOutputDir(), config.getFilenameTemplate()).toString();
    }


    private void validateRequest(DownloadRequest req) {
        String fmt = req.getFormat();
        if (fmt == null || (!fmt.equalsIgnoreCase("mp3") && !fmt.equalsIgnoreCase("mp4")))
            throw new IllegalArgumentException("Format must be mp3 or mp4");

        boolean hasStart = req.getStartSec() != null;
        boolean hasEnd = req.getEndSec() != null;

        if (hasStart != hasEnd)
            throw new IllegalArgumentException("Start and end seconds must be given together.");

        if (hasStart && req.getStartSec() < 0)
            throw new IllegalArgumentException("Start seconds cannot be negative.");

        if (hasStart && req.getEndSec() <= req.getStartSec())
            throw new IllegalArgumentException("End seconds must be greater than start.");

        if (hasStart && "mp3".equalsIgnoreCase(fmt))
            throw new IllegalArgumentException("Trim is only supported for mp4.");
    }

}