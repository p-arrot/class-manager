package com.example.edu.infrastructure.preview;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kkfileview")
public class PreviewProperties {
    private String baseUrl = "http://localhost:8012";
}
