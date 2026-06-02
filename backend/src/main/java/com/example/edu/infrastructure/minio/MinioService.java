package com.example.edu.infrastructure.minio;

import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            String bucket = properties.getBucket();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to check/create MinIO bucket", e);
            throw new BizException(ErrorCode.MINIO_ERROR, "MinIO bucket 初始化失败");
        }
    }

    public String generatePresignedPutUrl(String objectName, String contentType) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .expiry(10, TimeUnit.MINUTES)
                            .extraQueryParams(
                                    java.util.Map.of("Content-Type", contentType))
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT URL: objectName={}", objectName, e);
            throw new BizException(ErrorCode.MINIO_ERROR, "生成上传链接失败");
        }
    }

    public String generatePresignedGetUrl(String objectName) {
        return generatePresignedGetUrl(objectName, null);
    }

    public String generatePresignedGetUrl(String objectName, String fileName) {
        try {
            var builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .expiry(1, TimeUnit.HOURS);
            if (fileName != null && !fileName.isBlank()) {
                String encoded = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20");
                builder.extraQueryParams(java.util.Map.of(
                        "response-content-disposition",
                        "attachment; filename=\"" + encoded + "\""));
            }
            return minioClient.getPresignedObjectUrl(builder.build());
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL: objectName={}", objectName, e);
            throw new BizException(ErrorCode.MINIO_ERROR, "生成下载链接失败");
        }
    }

    public void deleteObject(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .build());
            log.info("MinIO object deleted: {}", objectName);
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object: objectName={}", objectName, e);
            throw new BizException(ErrorCode.MINIO_ERROR, "删除文件失败");
        }
    }

    public void uploadObject(String objectName, java.io.InputStream stream, String contentType) {
        try {
            minioClient.putObject(
                    io.minio.PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .stream(stream, -1, 209715200)
                            .contentType(contentType)
                            .build());
            log.info("MinIO object uploaded: {}", objectName);
        } catch (Exception e) {
            log.error("Failed to upload MinIO object: objectName={}", objectName, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR, "文件上传失败");
        }
    }

    public java.io.InputStream getObject(String objectName) {
        try {
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get MinIO object: objectName={}", objectName, e);
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    public boolean objectExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .build());
            return true;
        } catch (io.minio.errors.ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            log.error("MinIO statObject error: objectName={}", objectName, e);
            throw new BizException(ErrorCode.MINIO_ERROR, "文件服务异常");
        } catch (Exception e) {
            log.error("MinIO statObject unexpected error: objectName={}", objectName, e);
            throw new BizException(ErrorCode.MINIO_ERROR, "文件服务异常");
        }
    }
}
