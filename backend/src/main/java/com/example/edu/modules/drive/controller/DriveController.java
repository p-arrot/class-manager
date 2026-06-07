package com.example.edu.modules.drive.controller;

import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.result.R;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.infrastructure.preview.PreviewService;
import com.example.edu.modules.drive.dto.DriveRawFileDTO;
import com.example.edu.modules.drive.entity.DriveItem;
import com.example.edu.modules.drive.service.DriveService;
import com.example.edu.modules.drive.vo.DriveItemVO;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "学生网盘")
@RestController @RequestMapping("/api/drive") @RequiredArgsConstructor
public class DriveController {
    private final DriveService driveService;
    private final PreviewService previewService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<DriveItemVO> uploadFile(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "parentId", required = false) Long parentId) {
        Long uid = SecurityUtils.getCurrentUserId();
        String objectName = "drive/" + uid + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + file.getOriginalFilename();
        return R.ok(toVO(driveService.uploadFile(uid, file.getOriginalFilename(), file.getSize(),
                file.getContentType(), objectName, fileStream(file), parentId)));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<List<DriveItemVO>> tree(@RequestParam(required = false) Long parentId, @RequestParam(required = false) Long userId) {
        Long uid = userId != null ? userId : SecurityUtils.getCurrentUserId();
        List<DriveItem> items = driveService.list(uid, parentId);
        return R.ok(items.stream().map(this::toVO).toList());
    }

    @PostMapping("/folders")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<DriveItemVO> createFolder(@RequestBody Map<String,Object> body) {
        Long uid = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : SecurityUtils.getCurrentUserId();
        Long pid = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return R.ok(toVO(driveService.createFolder(uid, (String) body.get("name"), pid)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<Void> delete(@PathVariable Long id) { driveService.delete(id); return R.ok(); }

    @PostMapping("/files")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<DriveItemVO> uploadFile(@RequestBody Map<String,Object> body) {
        Long uid = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : SecurityUtils.getCurrentUserId();
        Long pid = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return R.ok(toVO(driveService.createFile(uid, (String) body.get("name"),
                body.get("fileSize") != null ? Long.valueOf(body.get("fileSize").toString()) : 0L,
                (String) body.get("contentType"), (String) body.get("objectName"), pid)));
    }

    @GetMapping("/{id}/raw")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<InputStreamResource> raw(@PathVariable Long id) {
        DriveRawFileDTO raw = driveService.getRawFile(id);
        String encoded = java.net.URLEncoder.encode(raw.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(raw.getContentType() != null ? raw.getContentType() : "application/octet-stream"))
                .contentLength(raw.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(encoded).build().toString())
                .body(new InputStreamResource(raw.getInputStream()));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<Map<String,String>> download(@PathVariable Long id) {
        return R.ok(Map.of("url", driveService.getDownloadUrl(id)));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<Map<String,String>> preview(@PathVariable Long id) {
        // Generate kkFileView preview URL (same flow as course resources)
        String minioUrl = driveService.getPreviewUrl(id);
        String kkViewUrl = previewService.generatePreviewUrl(minioUrl);
        return R.ok(Map.of("url", kkViewUrl));
    }

    // ========== helpers ==========

    private DriveItemVO toVO(DriveItem item) {
        if (item == null) return null;
        return DriveItemVO.builder()
                .id(item.getId())
                .name(item.getName())
                .type(item.getType())
                .fileSize(item.getFileSize())
                .contentType(item.getContentType())
                .parentId(item.getParentId())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private java.io.InputStream fileStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (java.io.IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR, "读取上传文件失败");
        }
    }
}
