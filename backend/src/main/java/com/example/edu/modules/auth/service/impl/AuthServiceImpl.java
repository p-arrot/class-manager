package com.example.edu.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.JwtUtils;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.auth.dto.LoginDTO;
import com.example.edu.modules.auth.service.AuthService;
import com.example.edu.modules.auth.vo.LoginVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user;

        if (loginDTO.getSchoolId() != null) {
            // 有 schoolId → 先查教师/管理员，再查学生
            user = loginAsStaffInSchool(loginDTO);
            if (user == null) {
                user = loginAsStudent(loginDTO);
            }
        } else {
            // 无 schoolId → 仅允许管理员登录
            user = loginAsAdmin(loginDTO);
        }

        if (user == null) {
            // 防止用户名枚举：执行一次无意义的BCrypt比对，消除时间侧信道
            passwordEncoder.matches(loginDTO.getPassword(),
                    "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234");
            throw new BizException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            log.warn("密码错误: account={}", loginDTO.getAccount());
            throw new BizException(ErrorCode.USERNAME_PASSWORD_ERROR);
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getUsername() != null ? user.getUsername() : user.getStudentNo(),
                user.getRole(),
                user.getSchoolId(),
                user.getClassId()
        );

        String token = jwtUtils.generateToken(loginUser);

        log.info("用户登录成功: userId={}, role={}", user.getId(), user.getRole());

        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(loginUser.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .schoolId(user.getSchoolId())
                .classId(user.getClassId())
                .build();
    }

    /**
     * 管理员登录（无 schoolId，仅按用户名查 role=admin）
     */
    private User loginAsAdmin(LoginDTO loginDTO) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getAccount())
                .eq(User::getRole, "admin"));
    }

    /**
     * 教师/管理员在指定学校内登录（按用户名 + schoolId）
     */
    private User loginAsStaffInSchool(LoginDTO loginDTO) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getAccount())
                .eq(User::getSchoolId, loginDTO.getSchoolId())
                .ne(User::getRole, "student"));
    }

    /**
     * 学生在指定学校内登录（按学号 + schoolId）
     */
    private User loginAsStudent(LoginDTO loginDTO) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, loginDTO.getAccount())
                .eq(User::getSchoolId, loginDTO.getSchoolId())
                .eq(User::getRole, "student"));
    }
}
