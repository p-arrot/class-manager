package com.example.edu.modules.audit.service;

public interface AuditLogService {

    /**
     * 记录审计日志。方法内部捕获所有异常，绝不会因为审计失败而影响主业务流程。
     *
     * @param action     操作描述（如"创建班级"）
     * @param targetType 目标类型（如"class"）
     * @param targetId   目标ID
     * @param detail     操作详情
     */
    void record(String action, String targetType, Long targetId, String detail);
}
