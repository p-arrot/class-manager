package com.example.edu.modules.drive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.infrastructure.minio.MinioService;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.drive.entity.DriveItem;
import com.example.edu.modules.drive.mapper.DriveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriveService {

    private final DriveMapper driveMapper;
    private final MinioService minioService;
    private final AuditLogService auditLogService;

    public List<DriveItem> list(Long userId, Long parentId) {
        LambdaQueryWrapper<DriveItem> wrapper = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getUserId, userId);
        if (parentId == null) wrapper.isNull(DriveItem::getParentId);
        else wrapper.eq(DriveItem::getParentId, parentId);
        return driveMapper.selectList(wrapper.orderByAsc(DriveItem::getType).orderByAsc(DriveItem::getName));
    }

    @Transactional
    public DriveItem createFolder(Long userId, String name, Long parentId) {
        DriveItem item = new DriveItem();
        item.setUserId(userId);
        item.setName(name);
        item.setType("FOLDER");
        item.setParentId(parentId);
        driveMapper.insert(item);
        return item;
    }

    @Transactional
    public DriveItem createFile(Long userId, String name, Long fileSize, String contentType, String objectName, Long parentId) {
        DriveItem item = new DriveItem();
        item.setUserId(userId);
        item.setName(name);
        item.setType("FILE");
        item.setFileSize(fileSize);
        item.setContentType(contentType);
        item.setObjectName(objectName);
        item.setParentId(parentId);
        driveMapper.insert(item);
        return item;
    }

    @Transactional
    public void delete(Long id) {
        deleteRecursive(id, new java.util.HashSet<>());
    }

    private void deleteRecursive(Long id, java.util.Set<Long> visited) {
        if (!visited.add(id)) return; // cyclic reference guard
        DriveItem item = driveMapper.selectById(id);
        if (item == null) throw new BizException(ErrorCode.NOT_FOUND);
        // Recursively delete children
        List<DriveItem> children = driveMapper.selectList(
                new LambdaQueryWrapper<DriveItem>().eq(DriveItem::getParentId, id));
        for (DriveItem child : children) deleteRecursive(child.getId(), visited);
        // Delete MinIO object if file
        if ("FILE".equals(item.getType()) && item.getObjectName() != null) {
            try { minioService.deleteObject(item.getObjectName()); } catch (Exception ignored) {}
        }
        driveMapper.deleteById(id);
        auditLogService.record("删除网盘文件", "user_drive", id, item.getName());
    }

    public String getDownloadUrl(Long id) {
        DriveItem item = driveMapper.selectById(id);
        if (item == null || !"FILE".equals(item.getType())) throw new BizException(ErrorCode.FILE_NOT_FOUND);
        return minioService.generatePresignedGetUrl(item.getObjectName(), item.getName());
    }

    public String getPreviewUrl(Long id) {
        DriveItem item = driveMapper.selectById(id);
        if (item == null || !"FILE".equals(item.getType())) throw new BizException(ErrorCode.FILE_NOT_FOUND);
        return minioService.generatePresignedGetUrl(item.getObjectName());
    }
}
