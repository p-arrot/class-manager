package com.example.edu.modules.course.service;

import com.example.edu.modules.course.dto.FileUploadDTO;
import com.example.edu.modules.course.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadVO createPresignedUpload(FileUploadDTO dto);

    FileUploadVO directUpload(FileUploadDTO dto, MultipartFile file);

    String getDownloadUrl(Long resourceId);

    String getPreviewUrl(Long resourceId);

    String getStreamUrl(Long resourceId);
}
