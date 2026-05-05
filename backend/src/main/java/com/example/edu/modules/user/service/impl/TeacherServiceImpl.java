package com.example.edu.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.entity.TeacherClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.user.dto.*;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import com.example.edu.modules.user.service.TeacherService;
import com.example.edu.modules.user.vo.TeacherClassVO;
import com.example.edu.modules.user.vo.TeacherVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final UserMapper userMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeacherVO create(TeacherCreateDTO dto) {
        // 检查用户名唯一性
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(ErrorCode.USERNAME_DUPLICATE);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setName(dto.getName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("teacher");
        user.setEnabled(true);
        userMapper.insert(user);

        auditLogService.record("创建教师", "teacher", user.getId(),
                "username=" + dto.getUsername() + ", name=" + dto.getName());

        return toVO(user, Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeacherVO update(Long id, TeacherUpdateDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, id)
                        .eq(User::getRole, "teacher"));
        if (user == null) {
            throw new BizException(ErrorCode.TEACHER_NOT_FOUND);
        }

        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }
        userMapper.updateById(user);

        List<Long> classIds = getClassIdsByTeacherId(id);

        auditLogService.record("更新教师", "teacher", id,
                "name=" + dto.getName());

        return toVO(userMapper.selectById(id), classIds);
    }

    @Override
    public IPage<TeacherVO> page(TeacherPageDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "teacher");
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, dto.getKeyword())
                    .or().like(User::getName, dto.getKeyword()));
        }
        wrapper.orderByAsc(User::getId);

        Page<User> page = new Page<>(dto.getPage(), dto.getSize());
        IPage<User> result = userMapper.selectPage(page, wrapper);

        // 批量获取所有教师的班级ID
        List<Long> teacherIds = result.getRecords().stream()
                .map(User::getId).toList();
        Map<Long, List<Long>> classIdMap = getClassIdMap(teacherIds);

        return result.convert(user -> toVO(user, classIdMap.getOrDefault(user.getId(), Collections.emptyList())));
    }

    @Override
    public TeacherVO getById(Long id) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, id)
                        .eq(User::getRole, "teacher"));
        if (user == null) {
            throw new BizException(ErrorCode.TEACHER_NOT_FOUND);
        }
        return toVO(user, getClassIdsByTeacherId(id));
    }

    @Override
    public List<TeacherClassVO> getTeacherClasses(Long teacherId) {
        User user = userMapper.selectById(teacherId);
        if (user == null || !"teacher".equals(user.getRole())) {
            throw new BizException(ErrorCode.TEACHER_NOT_FOUND);
        }

        List<TeacherClass> bindings = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId));

        if (CollectionUtils.isEmpty(bindings)) {
            return Collections.emptyList();
        }

        Set<Long> classIds = bindings.stream()
                .map(TeacherClass::getClassId).collect(Collectors.toSet());
        Map<Long, SchoolClass> classMap = schoolClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(SchoolClass::getId, sc -> sc));

        return bindings.stream()
                .map(tc -> {
                    SchoolClass sc = classMap.get(tc.getClassId());
                    return TeacherClassVO.builder()
                            .id(tc.getId())
                            .classId(tc.getClassId())
                            .grade(sc != null ? sc.getGrade() : null)
                            .className(sc != null ? sc.getName() : null)
                            .createdAt(tc.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchBind(Long teacherId, BatchBindDTO dto) {
        User user = userMapper.selectById(teacherId);
        if (user == null || !"teacher".equals(user.getRole())) {
            throw new BizException(ErrorCode.TEACHER_NOT_FOUND);
        }

        // 验证班级都存在
        List<SchoolClass> classes = schoolClassMapper.selectBatchIds(dto.getClassIds());
        if (classes.size() != dto.getClassIds().size()) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }

        int count = 0;
        for (Long classId : dto.getClassIds()) {
            // 检查是否已存在绑定
            long exists = teacherClassMapper.selectCount(
                    new LambdaQueryWrapper<TeacherClass>()
                            .eq(TeacherClass::getTeacherId, teacherId)
                            .eq(TeacherClass::getClassId, classId));
            if (exists == 0) {
                TeacherClass tc = new TeacherClass();
                tc.setTeacherId(teacherId);
                tc.setClassId(classId);
                teacherClassMapper.insert(tc);
                count++;
            }
        }

        auditLogService.record("批量绑定班级", "teacher", teacherId,
                "classIds: " + dto.getClassIds());

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnbind(Long teacherId, BatchUnbindDTO dto) {
        User user = userMapper.selectById(teacherId);
        if (user == null || !"teacher".equals(user.getRole())) {
            throw new BizException(ErrorCode.TEACHER_NOT_FOUND);
        }

        int count = teacherClassMapper.delete(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
                        .in(TeacherClass::getClassId, dto.getClassIds()));

        auditLogService.record("批量解绑班级", "teacher", teacherId,
                "classIds: " + dto.getClassIds());

        return count;
    }

    private List<Long> getClassIdsByTeacherId(Long teacherId) {
        return teacherClassMapper.selectList(
                        new LambdaQueryWrapper<TeacherClass>()
                                .eq(TeacherClass::getTeacherId, teacherId))
                .stream().map(TeacherClass::getClassId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || !"teacher".equals(user.getRole())) {
            throw new BizException(ErrorCode.TEACHER_NOT_FOUND);
        }
        // Remove class bindings first
        teacherClassMapper.delete(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, id));
        // Soft delete the user
        userMapper.deleteById(id);
        auditLogService.record("删除教师", "teacher", id, "username: " + user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, PasswordResetDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null || !"teacher".equals(user.getRole())) {
            throw new BizException(ErrorCode.TEACHER_NOT_FOUND);
        }
        String newPassword = (dto != null && dto.getNewPassword() != null && !dto.getNewPassword().isEmpty())
                ? dto.getNewPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        auditLogService.record("重置教师密码", "teacher", id, "username: " + user.getUsername());
    }

    private Map<Long, List<Long>> getClassIdMap(List<Long> teacherIds) {
        if (CollectionUtils.isEmpty(teacherIds)) {
            return Collections.emptyMap();
        }
        return teacherClassMapper.selectList(
                        new LambdaQueryWrapper<TeacherClass>()
                                .in(TeacherClass::getTeacherId, teacherIds))
                .stream()
                .collect(Collectors.groupingBy(
                        TeacherClass::getTeacherId,
                        Collectors.mapping(TeacherClass::getClassId, Collectors.toList())));
    }

    private TeacherVO toVO(User user, List<Long> classIds) {
        return TeacherVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .classIds(classIds)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
