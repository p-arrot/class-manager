package com.example.edu.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.infrastructure.minio.MinioService;
import com.example.edu.infrastructure.preview.PreviewService;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.dto.FileRawDTO;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private static final long MAX_COVER_SIZE = 5L * 1024 * 1024; // 5 MB
    private static final String COVER_PREFIX = "course-covers/";
    private static final java.util.Set<String> FORBIDDEN_EXTENSIONS =
            java.util.Set.of("exe", "bat", "sh", "cmd", "com", "msi", "dll", "so");
    private static final java.util.Set<String> ALLOWED_COVER_TYPES =
            java.util.Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private void validateFileType(String fileName) {
        if (fileName == null) return;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return;
        String ext = fileName.substring(dot + 1).toLowerCase();
        if (FORBIDDEN_EXTENSIONS.contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO createPresignedUpload(FileUploadDTO dto) {
        validateFileType(dto.getFileName());
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
        validateFileType(dto.getFileName());
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
                new LambdaQueryWrapper<CourseResource>()
                        .eq(dto.getParentId() != null, CourseResource::getParentId, dto.getParentId())
                        .isNull(dto.getParentId() == null, CourseResource::getParentId)
                        .eq(CourseResource::getCourseId, dto.getCourseId())
                        .orderByDesc(CourseResource::getSortOrder)
                        .last("LIMIT 1"));
        int nextSortOrder = siblings.isEmpty() ? 1 : siblings.get(0).getSortOrder() + 1;

        // 6. Upload file to MinIO. If the following DB transaction fails, remove
        // the object so a failed request cannot leave an unreachable file behind.
        boolean uploaded = false;
        try {
            minioService.uploadObject(objectName, file.getInputStream(), file.getContentType());
            uploaded = true;
        } catch (IOException e) {
            log.error("Failed to read upload stream: objectName={}", objectName, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR, "文件上传失败");
        }

        try {
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
            if (courseResourceMapper.insert(resource) != 1) {
                throw new BizException(ErrorCode.FILE_UPLOAD_ERROR, "文件记录保存失败");
            }

            auditLogService.record("上传文件", "course_resource", resource.getId(), resource.getName());

            return FileUploadVO.builder()
                    .resourceId(resource.getId())
                    .build();
        } catch (RuntimeException e) {
            if (uploaded) deleteObjectQuietly(objectName);
            throw e;
        }
    }

    @Override
    public FileUploadVO uploadCourseCover(MultipartFile file) {
        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("cover");
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        validateCover(fileName, contentType, file.getSize());
        String objectName = generateCoverObjectName(fileName);
        try {
            minioService.uploadObject(objectName, file.getInputStream(), contentType);
        } catch (IOException e) {
            log.error("Failed to read course cover stream: objectName={}", objectName, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR, "封面上传失败");
        }
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectName.getBytes(StandardCharsets.UTF_8));
        auditLogService.record("上传课程封面", "course_cover", null, fileName);
        return FileUploadVO.builder()
                .url("/api/files/course-cover/" + token)
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

    @Override
    public FileRawDTO getRawFile(Long resourceId) {
        CourseResource resource = loadFileResource(resourceId);
        java.io.InputStream stream = minioService.getObject(resource.getObjectName());
        return FileRawDTO.builder()
                .inputStream(stream)
                .contentType(resource.getContentType())
                .fileName(resource.getName())
                .fileSize(resource.getFileSize() != null ? resource.getFileSize() : 0)
                .build();
    }

    @Override
    public FileRawDTO getCourseCoverRaw(String token) {
        String objectName = decodeCoverToken(token);
        if (!objectName.startsWith(COVER_PREFIX)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        java.io.InputStream stream = minioService.getObject(objectName);
        return FileRawDTO.builder()
                .inputStream(stream)
                .contentType(inferImageContentType(objectName))
                .fileName(objectName.substring(objectName.lastIndexOf('/') + 1))
                .fileSize(0L)
                .build();
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

    private void deleteObjectQuietly(String objectName) {
        try {
            minioService.deleteObject(objectName);
        } catch (RuntimeException cleanupError) {
            log.error("Failed to clean up uploaded object after DB failure: objectName={}", objectName, cleanupError);
        }
    }

    private String generateObjectName(Long courseId, String fileName) {
        // Sanitize: strip path separators and control chars to prevent traversal
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
        String yearMonth = YearMonth.now().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return courseId + "/" + yearMonth + "/" + uuid + "_" + safeName;
    }

    private void validateCover(String fileName, String contentType, long fileSize) {
        validateFileType(fileName);
        if (fileSize <= 0 || fileSize > MAX_COVER_SIZE) {
            throw new BizException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (!ALLOWED_COVER_TYPES.contains(contentType)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 jpg、png、webp、gif 封面图片");
        }
    }

    private String generateCoverObjectName(String fileName) {
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
        String yearMonth = YearMonth.now().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return COVER_PREFIX + yearMonth + "/" + uuid + "_" + safeName;
    }

    private String decodeCoverToken(String token) {
        try {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    private String inferImageContentType(String objectName) {
        String lower = objectName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

}
