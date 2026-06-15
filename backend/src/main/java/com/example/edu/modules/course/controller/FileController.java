package com.example.edu.modules.course.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.course.dto.FileRawDTO;
import com.example.edu.modules.course.dto.FileUploadDTO;
import com.example.edu.modules.course.service.FileService;
import com.example.edu.modules.course.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "课程文件")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "获取预签名上传 URL")
    @PostMapping("/upload/presigned")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<FileUploadVO> getPresignedUploadUrl(@Valid @RequestBody FileUploadDTO dto) {
        return R.ok(fileService.createPresignedUpload(dto));
    }

    @Operation(summary = "直接上传文件（通过后端中转）")
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<FileUploadVO> directUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        FileUploadDTO dto = new FileUploadDTO();
        dto.setFileName(file.getOriginalFilename());
        dto.setContentType(file.getContentType());
        dto.setFileSize(file.getSize());
        dto.setCourseId(courseId);
        dto.setParentId(parentId);
        return R.ok(fileService.directUpload(dto, file));
    }

    @Operation(summary = "上传课程封面图片")
    @PostMapping("/course-cover/upload")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<FileUploadVO> uploadCourseCover(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.uploadCourseCover(file));
    }

    @Operation(summary = "读取课程封面图片")
    @GetMapping("/course-cover/{token}")
    public ResponseEntity<InputStreamResource> getCourseCover(@PathVariable String token) {
        FileRawDTO raw = fileService.getCourseCoverRaw(token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        raw.getContentType() != null ? raw.getContentType() : "image/jpeg"))
                .body(new InputStreamResource(raw.getInputStream()));
    }

    @Operation(summary = "获取文件下载 URL")
    @GetMapping("/{resourceId}/download")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<Map<String, String>> getDownloadUrl(@PathVariable Long resourceId) {
        String url = fileService.getDownloadUrl(resourceId);
        return R.ok(Map.of("url", url));
    }

    @Operation(summary = "获取文件预览 URL")
    @GetMapping("/{resourceId}/preview")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<Map<String, String>> getPreviewUrl(@PathVariable Long resourceId) {
        String url = fileService.getPreviewUrl(resourceId);
        return R.ok(Map.of("url", url));
    }

    @Operation(summary = "获取文件流 URL（无 attachment，用于 HTML 等内联渲染）")
    @GetMapping("/{resourceId}/stream")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<Map<String, String>> getStreamUrl(@PathVariable Long resourceId) {
        String url = fileService.getStreamUrl(resourceId);
        return R.ok(Map.of("url", url));
    }

    @Operation(summary = "直接下载文件（通过后端代理）")
    @GetMapping("/{resourceId}/raw")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<InputStreamResource> getRawFile(@PathVariable Long resourceId) {
        FileRawDTO raw = fileService.getRawFile(resourceId);
        String encodedFileName = java.net.URLEncoder.encode(raw.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        raw.getContentType() != null ? raw.getContentType() : "application/octet-stream"))
                .contentLength(raw.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(encodedFileName)
                                .build()
                                .toString())
                .body(new InputStreamResource(raw.getInputStream()));
    }
}
