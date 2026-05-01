package com.example.edu.modules.user.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.entity.TeacherClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.user.dto.PasswordResetDTO;
import com.example.edu.modules.user.dto.StudentExcelRowDTO;
import com.example.edu.modules.user.dto.StudentPageDTO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import com.example.edu.modules.user.service.StudentService;
import com.example.edu.modules.user.vo.StudentImportResultVO;
import com.example.edu.modules.user.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final UserMapper userMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentImportResultVO importStudents(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_EMPTY);
        }

        // 读取 Excel
        List<StudentExcelRowDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(StudentExcelRowDTO.class)
                    .sheet()
                    .doReadSync();
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_PARSE_ERROR);
        }

        if (CollectionUtils.isEmpty(rows)) {
            throw new BizException(ErrorCode.FILE_PARSE_ERROR, "文件中无数据");
        }

        LoginUser loginUser = getCurrentUser();
        Set<Long> myClassIds = null;
        if ("teacher".equals(loginUser.getRole())) {
            myClassIds = teacherClassMapper.selectList(
                            new LambdaQueryWrapper<TeacherClass>()
                                    .eq(TeacherClass::getTeacherId, loginUser.getUserId()))
                    .stream().map(TeacherClass::getClassId)
                    .collect(Collectors.toSet());
        }

        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        int successCount = 0;
        int failCount = 0;
        List<StudentImportResultVO.ImportError> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            StudentExcelRowDTO row = rows.get(i);
            int rowNum = i + 2; // Excel 行号（第1行为表头）
            try {
                // 校验必填字段
                if (!StringUtils.hasText(row.getGrade())
                        || !StringUtils.hasText(row.getClassName())
                        || !StringUtils.hasText(row.getStudentNo())
                        || !StringUtils.hasText(row.getName())) {
                    throw new BizException(ErrorCode.EXCEL_FORMAT_ERROR, "存在空字段");
                }

                // 查找或创建班级
                SchoolClass sc = findOrCreateClass(row.getGrade(), row.getClassName());

                // 教师权限检查
                if (myClassIds != null && !myClassIds.contains(sc.getId())) {
                    throw new BizException(ErrorCode.TEACHER_NOT_IN_CHARGE,
                            "您不负责班级: " + row.getGrade() + row.getClassName());
                }

                // 学号唯一性检查
                User existing = userMapper.selectOne(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getStudentNo, row.getStudentNo())
                                .eq(User::getRole, "student"));
                if (existing != null) {
                    throw new BizException(ErrorCode.STUDENT_NO_DUPLICATE,
                            "学号已存在: " + row.getStudentNo());
                }

                // 创建学生
                User student = new User();
                student.setStudentNo(row.getStudentNo());
                student.setName(row.getName());
                student.setPassword(encodedPassword);
                student.setRole("student");
                student.setClassId(sc.getId());
                student.setEnabled(true);
                userMapper.insert(student);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add(StudentImportResultVO.ImportError.builder()
                        .rowNum(rowNum)
                        .studentNo(row.getStudentNo())
                        .name(row.getName())
                        .errorMsg(e instanceof BizException ? e.getMessage() : "导入失败")
                        .build());
            }
        }

        auditLogService.record("导入学生", "student", null,
                "成功" + successCount + "条, 失败" + failCount + "条");

        return StudentImportResultVO.builder()
                .successCount(successCount)
                .failCount(failCount)
                .errors(errors)
                .build();
    }

    @Override
    public IPage<StudentVO> listStudents(StudentPageDTO dto) {
        LoginUser loginUser = getCurrentUser();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student");

        // 教师只能看到自己负责班级的学生
        if ("teacher".equals(loginUser.getRole())) {
            Set<Long> myClassIds = teacherClassMapper.selectList(
                            new LambdaQueryWrapper<TeacherClass>()
                                    .eq(TeacherClass::getTeacherId, loginUser.getUserId()))
                    .stream().map(TeacherClass::getClassId)
                    .collect(Collectors.toSet());
            if (myClassIds.isEmpty()) {
                return new Page<StudentVO>(dto.getPage(), dto.getSize(), 0);
            }
            if (dto.getClassId() != null && !myClassIds.contains(dto.getClassId())) {
                throw new BizException(ErrorCode.TEACHER_NOT_IN_CHARGE);
            }
            wrapper.in(User::getClassId, myClassIds);
        }

        if (dto.getClassId() != null) {
            wrapper.eq(User::getClassId, dto.getClassId());
        }

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(User::getName, dto.getKeyword())
                    .or().like(User::getStudentNo, dto.getKeyword()));
        }

        wrapper.orderByAsc(User::getClassId, User::getStudentNo);

        Page<User> page = new Page<>(dto.getPage(), dto.getSize());
        IPage<User> result = userMapper.selectPage(page, wrapper);

        // 批量获取班级信息
        Set<Long> classIds = result.getRecords().stream()
                .map(User::getClassId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SchoolClass> classMap = classIds.isEmpty() ? Collections.emptyMap()
                : schoolClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(SchoolClass::getId, sc -> sc));

        return result.convert(user -> toVO(user, classMap.get(user.getClassId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, PasswordResetDTO dto) {
        User student = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, id)
                        .eq(User::getRole, "student"));
        if (student == null) {
            throw new BizException(ErrorCode.STUDENT_NOT_FOUND);
        }

        // 教师权限检查
        LoginUser loginUser = getCurrentUser();
        if ("teacher".equals(loginUser.getRole())) {
            List<TeacherClass> bindings = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>()
                            .eq(TeacherClass::getTeacherId, loginUser.getUserId()));
            Set<Long> myClassIds = bindings.stream()
                    .map(TeacherClass::getClassId).collect(Collectors.toSet());
            if (student.getClassId() == null || !myClassIds.contains(student.getClassId())) {
                throw new BizException(ErrorCode.TEACHER_NOT_IN_CHARGE);
            }
        }

        String newPassword = StringUtils.hasText(dto.getNewPassword())
                ? dto.getNewPassword() : DEFAULT_PASSWORD;

        student.setPassword(passwordEncoder.encode(newPassword));
        int updated = userMapper.updateById(student);
        if (updated <= 0) {
            throw new BizException(ErrorCode.PASSWORD_RESET_FAILED);
        }

        log.info("密码已重置: studentId={}, operatorId={}", id, loginUser.getUserId());

        auditLogService.record("重置密码", "student", id,
                "重置学生 " + student.getName() + "(ID:" + id + ") 密码");
    }

    private SchoolClass findOrCreateClass(String grade, String className) {
        SchoolClass sc = schoolClassMapper.selectOne(
                new LambdaQueryWrapper<SchoolClass>()
                        .eq(SchoolClass::getGrade, grade)
                        .eq(SchoolClass::getName, className));
        if (sc == null) {
            sc = new SchoolClass();
            sc.setGrade(grade);
            sc.setName(className);
            schoolClassMapper.insert(sc);
        }
        return sc;
    }

    private LoginUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }

    private StudentVO toVO(User user, SchoolClass sc) {
        return StudentVO.builder()
                .id(user.getId())
                .studentNo(user.getStudentNo())
                .name(user.getName())
                .classId(user.getClassId())
                .grade(sc != null ? sc.getGrade() : null)
                .className(sc != null ? sc.getName() : null)
                .phone(user.getPhone())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
