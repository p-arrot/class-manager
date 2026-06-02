package com.example.edu.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.dto.CourseCreateDTO;
import com.example.edu.modules.course.dto.CoursePageDTO;
import com.example.edu.modules.course.dto.CourseUpdateDTO;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CourseService;
import com.example.edu.modules.course.vo.CourseDetailVO;
import com.example.edu.modules.course.vo.CourseVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final SemesterMapper semesterMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO create(CourseCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        checkNameDuplicate(dto.getName(), userId, null);

        Course course = new Course();
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setCoverUrl(dto.getCoverUrl());
        course.setTeacherId(userId);
        courseMapper.insert(course);

        if (!CollectionUtils.isEmpty(dto.getClassIds())) {
            for (Long classId : dto.getClassIds()) {
                CourseClass cc = new CourseClass();
                cc.setCourseId(course.getId());
                cc.setClassId(classId);
                courseClassMapper.insert(cc);
            }
        }

        auditLogService.record("创建课程", "course", course.getId(), course.getName());
        return toVO(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkTeacherOwnsCourse(course);

        // Check if course has semesters
        long semesterCount = semesterMapper.selectCount(
                new LambdaQueryWrapper<Semester>()
                        .eq(Semester::getCourseId, id));
        if (semesterCount > 0) {
            throw new BizException(ErrorCode.COURSE_HAS_SEMESTERS);
        }

        // Delete course-class bindings first
        courseClassMapper.delete(
                new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, id));

        courseMapper.deleteById(id);

        auditLogService.record("删除课程", "course", id, course.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO update(Long id, CourseUpdateDTO dto) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkTeacherOwnsCourse(course);

        if (StringUtils.hasText(dto.getName())) {
            checkNameDuplicate(dto.getName(), course.getTeacherId(), id);
            course.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        if (dto.getCoverUrl() != null) {
            course.setCoverUrl(dto.getCoverUrl());
        }
        courseMapper.updateById(course);

        if (dto.getClassIds() != null) {
            courseClassMapper.delete(
                    new LambdaQueryWrapper<CourseClass>()
                            .eq(CourseClass::getCourseId, id));
            if (!dto.getClassIds().isEmpty()) {
                for (Long classId : dto.getClassIds()) {
                    CourseClass cc = new CourseClass();
                    cc.setCourseId(id);
                    cc.setClassId(classId);
                    courseClassMapper.insert(cc);
                }
            }
        }

        auditLogService.record("更新课程", "course", id, course.getName());
        return toVO(courseMapper.selectById(id));
    }

    @Override
    public CourseDetailVO getById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkCourseAccess(course);

        // Query classIds
        List<CourseClass> bindings = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, id));
        List<Long> classIds = bindings.stream()
                .map(CourseClass::getClassId)
                .collect(Collectors.toList());

        // Query teacher name
        User teacher = userMapper.selectById(course.getTeacherId());
        String teacherName = teacher != null ? teacher.getName() : null;

        // Query semesters
        List<Semester> semesters = semesterMapper.selectList(
                new LambdaQueryWrapper<Semester>()
                        .eq(Semester::getCourseId, id)
                        .orderByDesc(Semester::getStartTime));
        List<com.example.edu.modules.course.vo.SemesterVO> semesterVOs = semesters.stream()
                .map(s -> com.example.edu.modules.course.vo.SemesterVO.builder()
                        .id(s.getId()).name(s.getName())
                        .startTime(s.getStartTime()).endTime(s.getEndTime())
                        .courseId(s.getCourseId()).lessonCount(0)
                        .createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt()).build())
                .collect(Collectors.toList());

        return CourseDetailVO.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .coverUrl(course.getCoverUrl())
                .teacherId(course.getTeacherId())
                .teacherName(teacherName)
                .classCount(classIds.size())
                .classIds(classIds)
                .semesters(semesterVOs)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    @Override
    public IPage<CourseVO> page(CoursePageDTO dto) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        String role = SecurityUtils.getCurrentUserRole();
        Long userId = SecurityUtils.getCurrentUserId();

        if ("teacher".equals(role)) {
            wrapper.eq(Course::getTeacherId, userId);
            if (dto.getClassId() != null) {
                List<CourseClass> bindings = courseClassMapper.selectList(
                        new LambdaQueryWrapper<CourseClass>()
                                .eq(CourseClass::getClassId, dto.getClassId()));
                List<Long> filteredIds = bindings.stream()
                        .map(CourseClass::getCourseId)
                        .distinct()
                        .collect(Collectors.toList());
                if (filteredIds.isEmpty()) {
                    wrapper.eq(Course::getId, -1L);
                } else {
                    wrapper.in(Course::getId, filteredIds);
                }
            }
        } else if ("student".equals(role)) {
            Long classId = SecurityUtils.getCurrentUserClassId();
            if (classId != null) {
                List<CourseClass> bindings = courseClassMapper.selectList(
                        new LambdaQueryWrapper<CourseClass>()
                                .eq(CourseClass::getClassId, classId));
                List<Long> courseIds = bindings.stream()
                        .map(CourseClass::getCourseId)
                        .distinct()
                        .collect(Collectors.toList());
                if (courseIds.isEmpty()) {
                    wrapper.eq(Course::getId, -1L);
                } else {
                    wrapper.in(Course::getId, courseIds);
                }
            } else {
                wrapper.eq(Course::getId, -1L);
            }
        }
        // Admin: no filter, sees all

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.like(Course::getName, dto.getKeyword());
        }
        wrapper.orderByDesc(Course::getCreatedAt);

        Page<Course> page = new Page<>(dto.getPage(), dto.getSize());
        IPage<Course> result = courseMapper.selectPage(page, wrapper);

        // Batch query teacher names
        Set<Long> teacherIds = result.getRecords().stream()
                .map(Course::getTeacherId)
                .collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = Map.of();
        if (!teacherIds.isEmpty()) {
            List<User> teachers = userMapper.selectBatchIds(teacherIds);
            teacherNameMap = teachers.stream()
                    .collect(Collectors.toMap(User::getId, User::getName));
        }

        // Batch query class counts
        Set<Long> courseIds = result.getRecords().stream()
                .map(Course::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> classCountMap = Map.of();
        if (!courseIds.isEmpty()) {
            List<CourseClass> bindings = courseClassMapper.selectList(
                    new LambdaQueryWrapper<CourseClass>()
                            .in(CourseClass::getCourseId, courseIds));
            classCountMap = bindings.stream()
                    .collect(Collectors.groupingBy(CourseClass::getCourseId,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        }

        Map<Long, String> finalTeacherNameMap = teacherNameMap;
        Map<Long, Integer> finalClassCountMap = classCountMap;
        return result.convert(course -> {
            return CourseVO.builder()
                    .id(course.getId())
                    .name(course.getName())
                    .description(course.getDescription())
                    .coverUrl(course.getCoverUrl())
                    .teacherId(course.getTeacherId())
                    .teacherName(finalTeacherNameMap.getOrDefault(course.getTeacherId(), null))
                    .classCount(finalClassCountMap.getOrDefault(course.getId(), 0))
                    .createdAt(course.getCreatedAt())
                    .updatedAt(course.getUpdatedAt())
                    .build();
        });
    }

    // ========== private helpers ==========

    private CourseVO toVO(Course course) {
        int classCount = courseClassMapper.selectCount(
                new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, course.getId())).intValue();

        User teacher = userMapper.selectById(course.getTeacherId());

        return CourseVO.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .coverUrl(course.getCoverUrl())
                .teacherId(course.getTeacherId())
                .teacherName(teacher != null ? teacher.getName() : null)
                .classCount(classCount)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private void checkNameDuplicate(String name, Long teacherId, Long excludeId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getName, name)
                .eq(Course::getTeacherId, teacherId);
        if (excludeId != null) {
            wrapper.ne(Course::getId, excludeId);
        }
        if (courseMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.COURSE_NAME_DUPLICATE);
        }
    }

    private void checkTeacherOwnsCourse(Course course) {
        String role = SecurityUtils.getCurrentUserRole();
        if (!"admin".equals(role) && !course.getTeacherId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    private void checkCourseAccess(Course course) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("admin".equals(role)) {
            return;
        }
        if ("teacher".equals(role)) {
            if (!course.getTeacherId().equals(SecurityUtils.getCurrentUserId())) {
                throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
            }
            return;
        }
        if ("student".equals(role)) {
            Long classId = SecurityUtils.getCurrentUserClassId();
            if (classId != null) {
                Long count = courseClassMapper.selectCount(
                        new LambdaQueryWrapper<CourseClass>()
                                .eq(CourseClass::getCourseId, course.getId())
                                .eq(CourseClass::getClassId, classId));
                if (count == 0) {
                    throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
                }
                return;
            }
        }
        throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
    }
}
