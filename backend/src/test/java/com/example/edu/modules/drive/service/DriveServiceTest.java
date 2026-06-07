package com.example.edu.modules.drive.service;

import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.LoginUser;
import com.example.edu.infrastructure.minio.MinioService;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.drive.entity.DriveItem;
import com.example.edu.modules.drive.mapper.DriveMapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveServiceTest {

    @Mock private DriveMapper driveMapper;
    @Mock private MinioService minioService;
    @Mock private AuditLogService auditLogService;
    @Mock private UserMapper userMapper;
    @Mock private TeacherClassMapper teacherClassMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentCannotListAnotherUsersDrive() {
        setUser(1L, "student");
        DriveService service = newService();

        assertThatThrownBy(() -> service.list(2L, null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        verifyNoInteractions(driveMapper, userMapper, teacherClassMapper);
    }

    @Test
    void teacherCanListDriveForStudentInManagedClass() {
        setUser(10L, "teacher");
        User student = student(2L, 7L);
        when(userMapper.selectById(2L)).thenReturn(student);
        when(teacherClassMapper.selectCount(any())).thenReturn(1L);
        when(driveMapper.selectList(any())).thenReturn(List.of());
        DriveService service = newService();

        assertThat(service.list(2L, null)).isEmpty();

        verify(userMapper).selectById(2L);
        verify(teacherClassMapper).selectCount(any());
    }

    @Test
    void teacherCannotListDriveForStudentOutsideManagedClasses() {
        setUser(10L, "teacher");
        when(userMapper.selectById(2L)).thenReturn(student(2L, 7L));
        when(teacherClassMapper.selectCount(any())).thenReturn(0L);
        DriveService service = newService();

        assertThatThrownBy(() -> service.list(2L, null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    @Test
    void adminCanListAnotherUsersDrive() {
        setUser(99L, "admin");
        when(driveMapper.selectList(any())).thenReturn(List.of());
        DriveService service = newService();

        assertThat(service.list(2L, null)).isEmpty();

        verifyNoInteractions(userMapper, teacherClassMapper);
    }

    @Test
    void studentCannotDownloadAnotherUsersFile() {
        setUser(1L, "student");
        when(driveMapper.selectById(20L)).thenReturn(file(20L, 2L));
        DriveService service = newService();

        assertThatThrownBy(() -> service.getRawFile(20L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        verifyNoInteractions(minioService);
    }

    private DriveService newService() {
        return new DriveService(driveMapper, minioService, auditLogService, userMapper, teacherClassMapper);
    }

    private static void setUser(Long userId, String role) {
        LoginUser loginUser = new LoginUser(userId, role + userId, role, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static User student(Long id, Long classId) {
        User user = new User();
        user.setId(id);
        user.setRole("student");
        user.setClassId(classId);
        return user;
    }

    private static DriveItem file(Long id, Long userId) {
        DriveItem item = new DriveItem();
        item.setId(id);
        item.setUserId(userId);
        item.setType("FILE");
        item.setObjectName("drive/" + userId + "/file.txt");
        item.setName("file.txt");
        return item;
    }
}
