package com.example.edu.modules.drive.controller;

import com.example.edu.common.result.R;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.infrastructure.minio.MinioService;
import com.example.edu.modules.drive.entity.DriveItem;
import com.example.edu.modules.drive.service.DriveService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "学生网盘")
@RestController @RequestMapping("/api/drive") @RequiredArgsConstructor
public class DriveController {
    private final DriveService driveService;
    private final MinioService minioService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<DriveItem> uploadFile(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "parentId", required = false) Long parentId) {
        Long uid = SecurityUtils.getCurrentUserId();
        String objectName = "drive/" + uid + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + file.getOriginalFilename();
        try {
            minioService.uploadObject(objectName, file.getInputStream(), file.getContentType());
        } catch (Exception e) {
            return R.fail(50002, "文件上传失败");
        }
        return R.ok(driveService.createFile(uid, file.getOriginalFilename(), file.getSize(),
                file.getContentType(), objectName, parentId));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<List<DriveItem>> tree(@RequestParam(required = false) Long parentId, @RequestParam(required = false) Long userId) {
        Long uid = userId != null ? userId : SecurityUtils.getCurrentUserId();
        return R.ok(driveService.list(uid, parentId));
    }

    @PostMapping("/folders")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<DriveItem> createFolder(@RequestBody Map<String,Object> body) {
        Long uid = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : SecurityUtils.getCurrentUserId();
        Long pid = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return R.ok(driveService.createFolder(uid, (String) body.get("name"), pid));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<Void> delete(@PathVariable Long id) { driveService.delete(id); return R.ok(); }

    @PostMapping("/files")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<DriveItem> uploadFile(@RequestBody Map<String,Object> body) {
        Long uid = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : SecurityUtils.getCurrentUserId();
        Long pid = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return R.ok(driveService.createFile(uid, (String) body.get("name"),
                body.get("fileSize") != null ? Long.valueOf(body.get("fileSize").toString()) : 0L,
                (String) body.get("contentType"), (String) body.get("objectName"), pid));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<Map<String,String>> download(@PathVariable Long id) {
        return R.ok(Map.of("url", driveService.getDownloadUrl(id)));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<Map<String,String>> preview(@PathVariable Long id) {
        return R.ok(Map.of("url", driveService.getPreviewUrl(id)));
    }
}
