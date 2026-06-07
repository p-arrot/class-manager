package com.example.edu.modules.drive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.infrastructure.minio.MinioService;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.TeacherClass;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.drive.dto.DriveRawFileDTO;
import com.example.edu.modules.drive.entity.DriveItem;
import com.example.edu.modules.drive.mapper.DriveMapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriveService {

    private final DriveMapper driveMapper;
    private final MinioService minioService;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;
    private final TeacherClassMapper teacherClassMapper;

    public List<DriveItem> list(Long userId, Long parentId) {
        Long ownerId = resolveDriveOwner(userId);
        validateParent(ownerId, parentId);
        LambdaQueryWrapper<DriveItem> wrapper = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getUserId, ownerId);
        if (parentId == null) wrapper.isNull(DriveItem::getParentId);
        else wrapper.eq(DriveItem::getParentId, parentId);
        return driveMapper.selectList(wrapper.orderByAsc(DriveItem::getType).orderByAsc(DriveItem::getName));
    }

    @Transactional
    public DriveItem createFolder(Long userId, String name, Long parentId) {
        Long ownerId = resolveDriveOwner(userId);
        validateParent(ownerId, parentId);
        DriveItem item = new DriveItem();
        item.setUserId(ownerId);
        item.setName(name);
        item.setType("FOLDER");
        item.setParentId(parentId);
        driveMapper.insert(item);
        return item;
    }

    @Transactional
    public DriveItem createFile(Long userId, String name, Long fileSize, String contentType, String objectName, Long parentId) {
        Long ownerId = resolveDriveOwner(userId);
        validateParent(ownerId, parentId);
        return createFileForOwner(ownerId, name, fileSize, contentType, objectName, parentId);
    }

    private DriveItem createFileForOwner(Long ownerId, String name, Long fileSize, String contentType, String objectName, Long parentId) {
        DriveItem item = new DriveItem();
        item.setUserId(ownerId);
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
    public DriveItem uploadFile(Long userId, String name, Long fileSize, String contentType, String objectName, java.io.InputStream stream, Long parentId) {
        Long ownerId = resolveDriveOwner(userId);
        validateParent(ownerId, parentId);
        minioService.uploadObject(objectName, stream, contentType);
        return createFileForOwner(ownerId, name, fileSize, contentType, objectName, parentId);
    }

    @Transactional
    public void delete(Long id) {
        deleteRecursive(id, new java.util.HashSet<>());
    }

    private void deleteRecursive(Long id, java.util.Set<Long> visited) {
        if (!visited.add(id)) return; // cyclic reference guard
        DriveItem item = driveMapper.selectById(id);
        if (item == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkItemAccess(item);
        // Recursively delete children
        List<DriveItem> children = driveMapper.selectList(
                new LambdaQueryWrapper<DriveItem>().eq(DriveItem::getParentId, id));
        for (DriveItem child : children) deleteRecursive(child.getId(), visited);
        // Delete MinIO object if file
        if ("FILE".equals(item.getType()) && item.getObjectName() != null) {
            deleteObjectQuietly(item.getObjectName());
        }
        driveMapper.deleteById(id);
        auditLogService.record("删除网盘文件", "user_drive", id, item.getName());
    }

    public String getDownloadUrl(Long id) {
        DriveItem item = loadFileItem(id);
        return minioService.generatePresignedGetUrl(item.getObjectName(), item.getName());
    }

    public String getPreviewUrl(Long id) {
        DriveItem item = loadFileItem(id);
        return minioService.generatePresignedGetUrl(item.getObjectName());
    }

    public DriveRawFileDTO getRawFile(Long id) {
        DriveItem item = loadFileItem(id);
        return DriveRawFileDTO.builder()
                .inputStream(minioService.getObject(item.getObjectName()))
                .contentType(item.getContentType())
                .fileName(item.getName())
                .fileSize(item.getFileSize() != null ? item.getFileSize() : 0)
                .build();
    }

    private DriveItem loadFileItem(Long id) {
        DriveItem item = driveMapper.selectById(id);
        if (item == null || !"FILE".equals(item.getType())) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        checkItemAccess(item);
        return item;
    }

    private Long resolveDriveOwner(Long requestedUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (requestedUserId == null || requestedUserId.equals(currentUserId)) {
            return currentUserId;
        }
        checkUserAccess(requestedUserId);
        return requestedUserId;
    }

    private void validateParent(Long ownerId, Long parentId) {
        if (parentId == null) return;
        DriveItem parent = driveMapper.selectById(parentId);
        if (parent == null || !"FOLDER".equals(parent.getType()) || !ownerId.equals(parent.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "父文件夹不存在");
        }
    }

    private void checkItemAccess(DriveItem item) {
        if (SecurityUtils.getCurrentUserId().equals(item.getUserId())) return;
        checkUserAccess(item.getUserId());
    }

    private void checkUserAccess(Long ownerId) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("admin".equals(role)) return;
        if ("teacher".equals(role) && isTeacherInCharge(ownerId)) return;
        throw new BizException(ErrorCode.FORBIDDEN, "无权访问他人网盘");
    }

    private boolean isTeacherInCharge(Long studentId) {
        User student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole()) || student.getClassId() == null) {
            return false;
        }
        Long count = teacherClassMapper.selectCount(new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getTeacherId, SecurityUtils.getCurrentUserId())
                .eq(TeacherClass::getClassId, student.getClassId()));
        return count != null && count > 0;
    }

    private void deleteObjectQuietly(String objectName) {
        try {
            minioService.deleteObject(objectName);
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object during drive cleanup: objectName={}", objectName, e);
        }
    }
}
