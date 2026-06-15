package com.example.edu.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.course.dto.SemesterCreateDTO;
import com.example.edu.modules.course.dto.SemesterUpdateDTO;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.SemesterService;
import com.example.edu.modules.course.vo.SemesterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {

    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final LessonMapper lessonMapper;
    private final CourseClassMapper courseClassMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SemesterVO create(Long courseId, SemesterCreateDTO dto) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkTeacherOwnsCourse(course);

        checkNameDuplicate(courseId, dto.getName(), null);
        validateTimeRange(courseId, dto.getStartTime(), dto.getEndTime(), null);

        Semester semester = new Semester();
        semester.setCourseId(courseId);
        semester.setName(dto.getName());
        semester.setStartTime(dto.getStartTime());
        semester.setEndTime(dto.getEndTime());
        semesterMapper.insert(semester);

        auditLogService.record("创建学期", "semester", semester.getId(), semester.getName());
        return toVO(semester);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Semester semester = semesterMapper.selectById(id);
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkTeacherOwnsCourse(course);

        long lessonCount = lessonMapper.selectCount(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getSemesterId, id));
        if (lessonCount > 0) {
            throw new BizException(ErrorCode.SEMESTER_HAS_LESSONS);
        }

        semesterMapper.deleteById(id);
        auditLogService.record("删除学期", "semester", id, semester.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SemesterVO update(Long id, SemesterUpdateDTO dto) {
        Semester semester = semesterMapper.selectById(id);
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkTeacherOwnsCourse(course);

        checkNameDuplicate(semester.getCourseId(), dto.getName(), id);
        validateTimeRange(semester.getCourseId(), dto.getStartTime(), dto.getEndTime(), id);

        semester.setName(dto.getName());
        semester.setStartTime(dto.getStartTime());
        semester.setEndTime(dto.getEndTime());
        semesterMapper.updateById(semester);

        auditLogService.record("更新学期", "semester", id, semester.getName());
        return toVO(semesterMapper.selectById(id));
    }

    @Override
    public SemesterVO getById(Long id) {
        Semester semester = semesterMapper.selectById(id);
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkCourseAccess(course);
        return toVO(semester);
    }

    @Override
    public List<SemesterVO> listByCourseId(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkCourseAccess(course);

        List<Semester> semesters = semesterMapper.selectList(
                new LambdaQueryWrapper<Semester>()
                        .eq(Semester::getCourseId, courseId)
                        .orderByDesc(Semester::getStartTime));

        // Batch query lesson counts
        Set<Long> semesterIds = semesters.stream()
                .map(Semester::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> lessonCountMap = Map.of();
        if (!semesterIds.isEmpty()) {
            List<Lesson> allLessons = lessonMapper.selectList(
                    new LambdaQueryWrapper<Lesson>()
                            .in(Lesson::getSemesterId, semesterIds));
            lessonCountMap = allLessons.stream()
                    .collect(Collectors.groupingBy(Lesson::getSemesterId,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        }

        Map<Long, Integer> finalLessonCountMap = lessonCountMap;
        return semesters.stream()
                .map(s -> toVO(s, finalLessonCountMap.getOrDefault(s.getId(), 0)))
                .toList();
    }

    // ========== private helpers ==========

    private SemesterVO toVO(Semester semester) {
        return toVO(semester, 0);
    }

    private SemesterVO toVO(Semester semester, int lessonCount) {
        return SemesterVO.builder()
                .id(semester.getId())
                .name(semester.getName())
                .startTime(semester.getStartTime())
                .endTime(semester.getEndTime())
                .courseId(semester.getCourseId())
                .lessonCount(lessonCount)
                .createdAt(semester.getCreatedAt())
                .updatedAt(semester.getUpdatedAt())
                .build();
    }

    private void checkNameDuplicate(Long courseId, String name, Long excludeId) {
        LambdaQueryWrapper<Semester> wrapper = new LambdaQueryWrapper<Semester>()
                .eq(Semester::getCourseId, courseId)
                .eq(Semester::getName, name);
        if (excludeId != null) {
            wrapper.ne(Semester::getId, excludeId);
        }
        if (semesterMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.SEMESTER_NAME_DUPLICATE);
        }
    }

    private void validateTimeRange(Long courseId, java.time.LocalDateTime start, java.time.LocalDateTime end, Long excludeId) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "学期开始时间必须早于结束时间");
        }
        LambdaQueryWrapper<Semester> wrapper = new LambdaQueryWrapper<Semester>()
                .eq(Semester::getCourseId, courseId)
                .lt(Semester::getStartTime, end)
                .gt(Semester::getEndTime, start);
        if (excludeId != null) {
            wrapper.ne(Semester::getId, excludeId);
        }
        if (semesterMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "同一课程下学期时间不能重叠");
        }
    }

    private void checkTeacherOwnsCourse(Course course) {
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
    }

    private void checkCourseAccess(Course course) {
        CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
    }
}
