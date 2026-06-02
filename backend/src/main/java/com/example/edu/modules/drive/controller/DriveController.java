package com.example.edu.modules.drive.controller;

import com.example.edu.common.result.R;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.drive.entity.DriveItem;
import com.example.edu.modules.drive.service.DriveService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "学生网盘")
@RestController @RequestMapping("/api/drive") @RequiredArgsConstructor
public class DriveController {
    private final DriveService driveService;

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

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public R<Map<String,String>> download(@PathVariable Long id) {
        return R.ok(Map.of("url", driveService.getDownloadUrl(id)));
    }
}
