package com.example.edu.modules.course.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class FileRawDTO {
    private InputStream inputStream;
    private String contentType;
    private String fileName;
    private long fileSize;
}
