package com.example.edu.modules.auth.service.impl;

import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.JwtUtils;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.auth.dto.LoginDTO;
import com.example.edu.modules.auth.vo.LoginVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void adminLoginSuccess() {
        User admin = buildUser(1L, "admin", "admin", "管理员", true);
        when(userMapper.selectOne(any())).thenReturn(admin);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");

        LoginVO result = authService.login(buildLogin("admin", "admin123"));

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getRole()).isEqualTo("admin");
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(jwtUtils).generateToken(argThat(LoginUser::isEnabled));
    }

    @Test
    void teacherLoginSuccess() {
        User teacher = buildUser(2L, "teacher1", "teacher", "张老师", true);
        teacher.setClassId(1L);
        when(userMapper.selectOne(any())).thenReturn(null, teacher);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");

        LoginVO result = authService.login(buildLogin("teacher1", "pass"));

        assertThat(result.getRole()).isEqualTo("teacher");
        assertThat(result.getClassId()).isEqualTo(1L);
    }

    @Test
    void studentLoginSuccess() {
        User student = new User();
        student.setId(3L);
        student.setStudentNo("2026001");
        student.setName("小明");
        student.setRole("student");
        student.setPassword("encoded-pass");
        student.setEnabled(true);
        student.setClassId(1L);
        when(userMapper.selectOne(any())).thenReturn(null, null, student);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");

        LoginVO result = authService.login(buildLogin("2026001", "pass"));

        assertThat(result.getRole()).isEqualTo("student");
        assertThat(result.getUsername()).isEqualTo("2026001");
    }

    @Test
    void wrongPasswordThrows() {
        User user = buildUser(1L, "admin", "admin", "管理员", true);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(buildLogin("admin", "wrong")))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.USERNAME_PASSWORD_ERROR.getCode());
    }

    @Test
    void disabledAccountThrows() {
        User user = buildUser(1L, "admin", "admin", "管理员", false);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(buildLogin("admin", "admin123")))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.ACCOUNT_DISABLED.getCode());

        // Verify no token was generated for disabled user
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    void userNotFoundThrows() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(buildLogin("nobody", "pass")))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.USERNAME_PASSWORD_ERROR.getCode());

        // Verify timing-attack mitigation: BCrypt was called even on failed lookup
        verify(passwordEncoder).matches(eq("pass"), anyString());
    }

    @Test
    void enabledUserPassesEnabledFieldToToken() {
        User admin = buildUser(1L, "admin", "admin", "管理员", true);
        when(userMapper.selectOne(any())).thenReturn(admin);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");

        authService.login(buildLogin("admin", "admin123"));

        verify(jwtUtils).generateToken(argThat(lu -> lu instanceof LoginUser && lu.isEnabled()));
    }

    private static User buildUser(Long id, String username, String role, String name, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setName(name);
        user.setPassword("encoded-pass");
        user.setEnabled(enabled);
        return user;
    }

    private static LoginDTO buildLogin(String account, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setAccount(account);
        dto.setPassword(password);
        return dto;
    }
}
