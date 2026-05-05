package com.example.edu.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.infrastructure.minio.MinioService;
import com.example.edu.infrastructure.preview.PreviewService;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.dto.FileUploadDTO;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.CourseResource;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.CourseResourceMapper;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.course.service.FileService;
import com.example.edu.modules.course.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final CourseResourceMapper courseResourceMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final MinioService minioService;
    private final PreviewService previewService;
    private final AuditLogService auditLogService;

    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024; // 200 MB

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO createPresignedUpload(FileUploadDTO dto) {
        // 1. Validate course
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        CoursePermissionHelper.checkTeacherOwnsCourse(course);

        // 2. Validate file size
        if (dto.getFileSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 3. Validate parent folder if specified
        if (dto.getParentId() != null) {
            CourseResource parent = courseResourceMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getCourseId().equals(dto.getCourseId())) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        // 4. Generate object name
        String objectName = generateObjectName(dto.getCourseId(), dto.getFileName());

        // 5. Calculate sort order (append to end of siblings)
        List<CourseResource> siblings = courseResourceMapper.selectList(
                new LambdaQueryWrapper<CourseResource>()
                        .eq(dto.getParentId() != null,
                                CourseResource::getParentId, dto.getParentId())
                        .isNull(dto.getParentId() == null,
                                CourseResource::getParentId)
                        .eq(CourseResource::getCourseId, dto.getCourseId())
                        .orderByDesc(CourseResource::getSortOrder)
                        .last("LIMIT 1"));
        int nextSortOrder = siblings.isEmpty() ? 1 : siblings.get(0).getSortOrder() + 1;

        // 6. Generate presigned PUT URL
        String presignedUrl = minioService.generatePresignedPutUrl(objectName, dto.getContentType());

        // 7. Create CourseResource record (type=FILE)
        CourseResource resource = new CourseResource();
        resource.setCourseId(dto.getCourseId());
        resource.setParentId(dto.getParentId());
        resource.setName(dto.getFileName());
        resource.setType("FILE");
        resource.setSortOrder(nextSortOrder);
        resource.setFileSize(dto.getFileSize());
        resource.setContentType(dto.getContentType());
        resource.setObjectName(objectName);
        courseResourceMapper.insert(resource);

        // 8. Audit log
        auditLogService.record("上传文件", "course_resource", resource.getId(), resource.getName());

        return FileUploadVO.builder()
                .resourceId(resource.getId())
                .presignedUrl(presignedUrl)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO directUpload(FileUploadDTO dto, MultipartFile file) {
        // 1. Validate course
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkTeacherOwnsCourse(course);

        // 2. Validate file size
        if (dto.getFileSize() > MAX_FILE_SIZE) throw new BizException(ErrorCode.FILE_SIZE_EXCEEDED);

        // 3. Validate parent folder
        if (dto.getParentId() != null) {
            CourseResource parent = courseResourceMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getCourseId().equals(dto.getCourseId()))
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 4. Generate object name
        String objectName = generateObjectName(dto.getCourseId(), dto.getFileName());

        // 5. Calculate sort order
        List<CourseResource> siblings = courseResourceMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseResource>()
                        .eq(dto.getParentId() != null, CourseResource::getParentId, dto.getParentId())
                        .isNull(dto.getParentId() == null, CourseResource::getParentId)
                        .eq(CourseResource::getCourseId, dto.getCourseId())
                        .orderByDesc(CourseResource::getSortOrder)
                        .last("LIMIT 1"));
        int nextSortOrder = siblings.isEmpty() ? 1 : siblings.get(0).getSortOrder() + 1;

        // 6. Upload file to MinIO
        try {
            minioService.uploadObject(objectName, file.getInputStream(), file.getContentType());
        } catch (Exception e) {
            log.error("Failed to upload to MinIO: objectName={}", objectName, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR, "文件上传失败");
        }

        // 7. Create DB record
        CourseResource resource = new CourseResource();
        resource.setCourseId(dto.getCourseId());
        resource.setParentId(dto.getParentId());
        resource.setName(dto.getFileName());
        resource.setType("FILE");
        resource.setSortOrder(nextSortOrder);
        resource.setFileSize(dto.getFileSize());
        resource.setContentType(dto.getContentType());
        resource.setObjectName(objectName);
        courseResourceMapper.insert(resource);

        auditLogService.record("上传文件", "course_resource", resource.getId(), resource.getName());

        return FileUploadVO.builder()
                .resourceId(resource.getId())
                .build();
    }

    @Override
    public String getDownloadUrl(Long resourceId) {
        CourseResource resource = loadFileResource(resourceId);
        // Generate download URL with forced attachment (prevents browser from rendering)
        return minioService.generatePresignedGetUrl(resource.getObjectName(), resource.getName());
    }

    @Override
    public String getPreviewUrl(Long resourceId) {
        CourseResource resource = loadFileResource(resourceId);
        // Preview URL: NO attachment header (kkFileView needs to read the raw file)
        String presignedGetUrl = minioService.generatePresignedGetUrl(resource.getObjectName());
        return previewService.generatePreviewUrl(presignedGetUrl);
    }

    @Override
    public String getStreamUrl(Long resourceId) {
        CourseResource resource = loadFileResource(resourceId);
        // Stream URL: NO attachment header (for inline HTML/image rendering)
        return minioService.generatePresignedGetUrl(resource.getObjectName());
    }

    // ========== private helpers ==========

    private CourseResource loadFileResource(Long resourceId) {
        CourseResource resource = courseResourceMapper.selectById(resourceId);
        if (resource == null || !"FILE".equals(resource.getType())) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        Course course = courseMapper.selectById(resource.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
        return resource;
    }

    private String generateObjectName(Long courseId, String fileName) {
        // Sanitize: strip path separators and control chars to prevent traversal
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
        String yearMonth = YearMonth.now().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return courseId + "/" + yearMonth + "/" + uuid + "_" + safeName;
    }

}
