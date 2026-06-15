package com.example.edu.modules.audit.service.impl;

import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.audit.entity.AuditLog;
import com.example.edu.modules.audit.mapper.AuditLogMapper;
import com.example.edu.modules.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void record(String action, String targetType, Long targetId, String detail) {
        try {
            Long userId = null;
            String ip = null;
            String userAgent = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())
                    && auth.getPrincipal() instanceof LoginUser loginUser) {
                userId = loginUser.getUserId();
            }

            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes attrs) {
                HttpServletRequest request = attrs.getRequest();
                ip = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
            }

            AuditLog logEntry = new AuditLog();
            logEntry.setUserId(userId);
            logEntry.setAction(action);
            logEntry.setTargetType(targetType);
            logEntry.setTargetId(targetId);
            logEntry.setDetail(detail);
            logEntry.setIp(ip);
            logEntry.setUserAgent(userAgent);
            auditLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("审计日志写入失败: action={}", action, e);
        }
    }
}
