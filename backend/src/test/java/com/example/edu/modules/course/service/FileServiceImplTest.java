package com.example.edu.modules.course.service;

import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.infrastructure.minio.MinioService;
import com.example.edu.infrastructure.preview.PreviewService;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.CourseResourceMapper;
import com.example.edu.modules.course.service.impl.FileServiceImpl;
import com.example.edu.modules.course.vo.FileUploadVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock private CourseResourceMapper courseResourceMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private CourseClassMapper courseClassMapper;
    @Mock private MinioService minioService;
    @Mock private PreviewService previewService;
    @Mock private AuditLogService auditLogService;

    @Test
    void uploadCourseCoverStoresImageUnderStableCoverPath() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("cover.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        FileServiceImpl service = newService();

        FileUploadVO result = service.uploadCourseCover(file);

        assertThat(result.getResourceId()).isNull();
        assertThat(result.getUrl()).startsWith("/api/files/course-cover/");
        ArgumentCaptor<String> objectName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> inputStream = ArgumentCaptor.forClass(InputStream.class);
        verify(minioService).uploadObject(objectName.capture(), inputStream.capture(), eq("image/png"));
        assertThat(inputStream.getValue()).isNotNull();
        assertThat(objectName.getValue()).startsWith("course-covers/");
        assertThat(objectName.getValue()).endsWith("_cover.png");
        verifyNoInteractions(courseResourceMapper, courseMapper, courseClassMapper);
    }

    @Test
    void uploadCourseCoverRejectsNonImage() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("cover.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getSize()).thenReturn(1024L);
        FileServiceImpl service = newService();

        assertThatThrownBy(() -> service.uploadCourseCover(file))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode());
        verifyNoInteractions(minioService);
    }

    @Test
    void uploadCourseCoverRejectsOversizedImage() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("cover.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(6L * 1024 * 1024);
        FileServiceImpl service = newService();

        assertThatThrownBy(() -> service.uploadCourseCover(file))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getCode());
        verifyNoInteractions(minioService);
    }

    @Test
    void getCourseCoverRawDecodesStableToken() {
        String objectName = "course-covers/2026-06/abc_cover.webp";
        String token = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectName.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        InputStream stream = new ByteArrayInputStream(new byte[] {1});
        when(minioService.getObject(objectName)).thenReturn(stream);
        FileServiceImpl service = newService();

        var raw = service.getCourseCoverRaw(token);

        assertThat(raw.getInputStream()).isSameAs(stream);
        assertThat(raw.getContentType()).isEqualTo("image/webp");
        assertThat(raw.getFileName()).isEqualTo("abc_cover.webp");
    }

    private FileServiceImpl newService() {
        return new FileServiceImpl(
                courseResourceMapper,
                courseMapper,
                courseClassMapper,
                minioService,
                previewService,
                auditLogService);
    }
}
