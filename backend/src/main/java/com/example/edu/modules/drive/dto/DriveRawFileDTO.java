package com.example.edu.modules.drive.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class DriveRawFileDTO {
    private InputStream inputStream;
    private String contentType;
    private String fileName;
    private long fileSize;
}
