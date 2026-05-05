package com.example.edu.infrastructure.preview;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(PreviewProperties.class)
public class PreviewService {

    private final PreviewProperties properties;

    public String generatePreviewUrl(String presignedGetUrl) {
        // kkFileView expects the URL parameter to be standard base64-encoded
        String encoded = Base64.getEncoder().encodeToString(
                presignedGetUrl.getBytes(StandardCharsets.UTF_8));
        return properties.getBaseUrl() + "/onlinePreview?url=" + encoded;
    }
}
