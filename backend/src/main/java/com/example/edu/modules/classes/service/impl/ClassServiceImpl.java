package com.example.edu.modules.classes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.dto.ClassCreateDTO;
import com.example.edu.modules.classes.dto.ClassUpdateDTO;
import com.example.edu.modules.classes.dto.ClassPageDTO;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.entity.TeacherClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.classes.mapper.TeacherClassMapper;
import com.example.edu.modules.classes.service.ClassService;
import com.example.edu.modules.classes.vo.ClassVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final SchoolClassMapper schoolClassMapper;
    private final UserMapper userMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassVO create(ClassCreateDTO dto) {
        // 检查同年级下班级名唯一
        checkNameDuplicate(dto.getGrade(), dto.getName(), null);

        SchoolClass sc = new SchoolClass();
        sc.setGrade(dto.getGrade());
        sc.setName(dto.getName());
        schoolClassMapper.insert(sc);

        auditLogService.record("创建班级", "class", sc.getId(),
                dto.getGrade() + dto.getName());

        return toVO(sc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SchoolClass sc = schoolClassMapper.selectById(id);
        if (sc == null) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }

        // 检查班级下是否有学生
        long studentCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getClassId, id)
                        .eq(User::getRole, "student"));
        if (studentCount > 0) {
            throw new BizException(ErrorCode.CLASS_HAS_STUDENTS);
        }

        // 检查是否有关联教师
        long teacherCount = teacherClassMapper.selectCount(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getClassId, id));
        if (teacherCount > 0) {
            throw new BizException(ErrorCode.CLASS_HAS_TEACHERS);
        }

        schoolClassMapper.deleteById(id);

        auditLogService.record("删除班级", "class", id,
                sc.getGrade() + sc.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassVO update(Long id, ClassUpdateDTO dto) {
        SchoolClass sc = schoolClassMapper.selectById(id);
        if (sc == null) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }

        checkNameDuplicate(dto.getGrade(), dto.getName(), id);

        sc.setGrade(dto.getGrade());
        sc.setName(dto.getName());
        schoolClassMapper.updateById(sc);

        auditLogService.record("更新班级", "class", id,
                dto.getGrade() + dto.getName());

        return toVO(schoolClassMapper.selectById(id));
    }

    @Override
    public ClassVO getById(Long id) {
        SchoolClass sc = schoolClassMapper.selectById(id);
        if (sc == null) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }
        return toVO(sc);
    }

    @Override
    public IPage<ClassVO> page(ClassPageDTO dto) {
        LambdaQueryWrapper<SchoolClass> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getGrade())) {
            wrapper.eq(SchoolClass::getGrade, dto.getGrade());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.like(SchoolClass::getName, dto.getKeyword());
        }
        wrapper.orderByAsc(SchoolClass::getGrade, SchoolClass::getName);

        Page<SchoolClass> page = new Page<>(dto.getPage(), dto.getSize());
        IPage<SchoolClass> result = schoolClassMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public List<ClassVO> listAll() {
        LambdaQueryWrapper<SchoolClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SchoolClass::getGrade, SchoolClass::getName);
        return schoolClassMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    private void checkNameDuplicate(String grade, String name, Long excludeId) {
        LambdaQueryWrapper<SchoolClass> wrapper = new LambdaQueryWrapper<SchoolClass>()
                .eq(SchoolClass::getGrade, grade)
                .eq(SchoolClass::getName, name);
        if (excludeId != null) {
            wrapper.ne(SchoolClass::getId, excludeId);
        }
        if (schoolClassMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.CLASS_NAME_DUPLICATE);
        }
    }

    private ClassVO toVO(SchoolClass sc) {
        return ClassVO.builder()
                .id(sc.getId())
                .grade(sc.getGrade())
                .name(sc.getName())
                .createdAt(sc.getCreatedAt())
                .updatedAt(sc.getUpdatedAt())
                .build();
    }
}
