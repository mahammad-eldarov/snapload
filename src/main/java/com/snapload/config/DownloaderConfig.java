package com.snapload.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "downloader")
public class DownloaderConfig {

    private String outputDir = "./downloads";
    private String audioFormat = "mp3";
    private String videoFormat = "mp4";
    private int videoQuality = 1080;
    private int audioQuality = 0;
    private boolean embedThumbnail = true;
    private boolean addMetadata = true;
    private String filenameTemplate = "%(title)s";
    private String ytDlpCommand = "yt-dlp";
    private String ffmpegCommand = "ffmpeg";


}

