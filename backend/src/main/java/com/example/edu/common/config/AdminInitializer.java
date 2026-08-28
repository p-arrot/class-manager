package com.example.edu.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin-password:admin123}")
    private String initialAdminPassword;

    @Override
    public void run(String... args) {
        Long adminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "admin"));

        if (adminCount == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setName("系统管理员");
            admin.setPassword(passwordEncoder.encode(initialAdminPassword));
            admin.setRole("admin");
            admin.setEnabled(true);
            userMapper.insert(admin);
            log.warn("默认管理员账号已创建，请立即修改初始密码");
        } else {
            log.info("管理员账号已存在，跳过初始化");
        }
    }
}
