package com.ytmp3.service;

import com.ytmp3.config.DownloaderConfig;
import com.ytmp3.exception.DownloadException;
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

    public ResponseBodyEmitter streamDownload(String url) {

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {

                Process process = startProcess(url);

                streamProcess(process, line -> {
                    try {

                        emitter.send(line + "\n");

                        if (line.contains("STATUS:DONE")) {
                            emitter.send("Done!\n");
                        }

                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });

                int exitCode = process.waitFor();

                CompletableFuture.runAsync(this::deleteTempFiles);

                if (exitCode == 0) {
                    emitter.send("STATUS:DONE\n");
                }

                emitter.complete();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    public String download(String url) {

        validateUrl(url);
        checkDependencies();
        createOutputDir();

        log.info("Download started: {}", url);

        List<String> command = buildCommand(url);

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            String output = readProcessOutput(process);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new DownloadException("yt-dlp failed:\n" + output);
            }

            String fileName = extractFileName(output);

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

    public Process startProcess(String url) throws IOException {
        validateUrl(url);
        checkDependencies();
        createOutputDir();

        List<String> command = buildCommand(url);

        return new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
    }

    public void streamProcess(Process process, Consumer<String> consumer) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        )) {

            String line;

            while ((line = reader.readLine()) != null) {
                log.info("[yt-dlp] {}", line);
                consumer.accept(line);
            }
        }
    }

    public List<String> buildCommand(String url) {

        List<String> cmd = new ArrayList<>();

        cmd.add(config.getYtDlpCommand());

        cmd.add("--extract-audio");
        cmd.add("--audio-format");
        cmd.add(config.getAudioFormat());

        cmd.add("--audio-quality");
        cmd.add(String.valueOf(config.getAudioQuality()));

        if (config.isEmbedThumbnail()) {
            cmd.add("--embed-thumbnail");
        }

        if (config.isAddMetadata()) {
            cmd.add("--add-metadata");
        }

        cmd.add("--output");
        cmd.add(buildOutputTemplate());

        cmd.add(url);

        return cmd;
    }

    public void deleteTempFiles() {

        Path tempPath = Paths.get("tmp");

        try {
            if (!Files.exists(tempPath)) return;

            try (Stream<Path> files = Files.walk(tempPath)) {
                files
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                Files.deleteIfExists(file);
                                log.info("Deleted: {}", file.getFileName());
                            } catch (IOException e) {
                                log.error("Delete failed: {}", file, e);
                            }
                        });
            }

        } catch (Exception e) {
            log.error("Cleanup failed", e);
        }
    }

    private String readProcessOutput(Process process) throws IOException {

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        )) {

            String line;

            while ((line = reader.readLine()) != null) {
                log.info("[yt-dlp] {}", line);
                output.append(line).append("\n");
            }
        }

        return output.toString();
    }

    private void validateUrl(String url) {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL empty");
        }

        String regex = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$";

        if (!url.matches(regex)) {
            throw new IllegalArgumentException("Invalid YouTube URL");
        }
    }


    private void checkDependencies() {
        checkTool(config.getYtDlpCommand());
    }

    private void checkTool(String tool) {
        try {
            int exit = new ProcessBuilder(tool, "--version")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();

            if (exit != 0) {
                throw new DownloadException(tool + " not working");
            }

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new DownloadException(tool + " not found");
        }
    }

    private String extractFileName(String output) {

        String last = null;

        for (String line : output.split("\n")) {
            if (line.contains("Destination:")) {
                last = line.substring(line.indexOf("Destination:") + 13).trim();
            }
        }

        if (last != null) {
            return last;
        }

        return "output_" + System.currentTimeMillis() + ".mp3";
    }

    private void createOutputDir() {
        try {
            Files.createDirectories(
                    Path.of(config.getOutputDir())
            );
        } catch (IOException e) {
            log.warn("Folder error: {}", e.getMessage());
        }
    }

    private String buildOutputTemplate() {
        return Path.of(
                config.getOutputDir(),
                config.getFilenameTemplate()
        ).toString();
    }
}