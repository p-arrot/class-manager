package com.example.edu.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.dto.LessonCreateDTO;
import com.example.edu.modules.course.dto.LessonSortDTO;
import com.example.edu.modules.course.dto.LessonUpdateDTO;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.LessonService;
import com.example.edu.modules.course.vo.LessonVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonMapper lessonMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LessonVO create(Long semesterId, LessonCreateDTO dto) {
        Semester semester = semesterMapper.selectById(semesterId);
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        checkSemesterOwner(semester);

        // Calculate next sort order
        List<Lesson> existing = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getSemesterId, semesterId)
                        .orderByDesc(Lesson::getSortOrder)
                        .last("LIMIT 1"));
        int nextSortOrder = existing.isEmpty() ? 1 : existing.get(0).getSortOrder() + 1;

        Lesson lesson = new Lesson();
        lesson.setSemesterId(semesterId);
        lesson.setName(dto.getName());
        lesson.setSortOrder(nextSortOrder);
        lessonMapper.insert(lesson);

        auditLogService.record("创建课时", "lesson", lesson.getId(), lesson.getName());
        return toVO(lesson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        checkLessonOwner(lesson);

        lessonMapper.deleteById(id);
        auditLogService.record("删除课时", "lesson", id, lesson.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LessonVO update(Long id, LessonUpdateDTO dto) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        checkLessonOwner(lesson);

        lesson.setName(dto.getName());
        lessonMapper.updateById(lesson);

        auditLogService.record("更新课时", "lesson", id, lesson.getName());
        return toVO(lessonMapper.selectById(id));
    }

    @Override
    public LessonVO getById(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        // Verify access by tracing back to course
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        String role = SecurityUtils.getCurrentUserRole();
        if ("teacher".equals(role)) {
            if (!course.getTeacherId().equals(SecurityUtils.getCurrentUserId())) {
                throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
            }
        } else if ("student".equals(role)) {
            Long classId = SecurityUtils.getCurrentUserClassId();
            if (classId != null) {
                Long count = courseClassMapper.selectCount(
                        new LambdaQueryWrapper<CourseClass>()
                                .eq(CourseClass::getCourseId, course.getId())
                                .eq(CourseClass::getClassId, classId));
                if (count == 0) {
                    throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
                }
            }
        }
        return toVO(lesson);
    }

    @Override
    public List<LessonVO> listBySemesterId(Long semesterId) {
        Semester semester = semesterMapper.selectById(semesterId);
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        checkSemesterOwner(semester);
        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getSemesterId, semesterId)
                        .orderByAsc(Lesson::getSortOrder));
        return lessons.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorder(Long id, LessonSortDTO dto) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        checkLessonOwner(lesson);

        List<Lesson> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getSemesterId, lesson.getSemesterId())
                        .orderByAsc(Lesson::getSortOrder));

        // Remove the target lesson from list
        lessons.removeIf(l -> l.getId().equals(id));

        // Insert at target position
        int targetIndex = Math.max(0, Math.min(dto.getTargetIndex(), lessons.size()));
        lessons.add(targetIndex, lesson);

        // Renumber sort_order
        for (int i = 0; i < lessons.size(); i++) {
            Lesson l = lessons.get(i);
            if (!l.getSortOrder().equals(i + 1)) {
                l.setSortOrder(i + 1);
                lessonMapper.updateById(l);
            }
        }
    }

    // ========== private helpers ==========

    private LessonVO toVO(Lesson lesson) {
        return LessonVO.builder()
                .id(lesson.getId())
                .name(lesson.getName())
                .sortOrder(lesson.getSortOrder())
                .semesterId(lesson.getSemesterId())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    private void checkSemesterOwner(Semester semester) {
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        String role = SecurityUtils.getCurrentUserRole();
        if (!"admin".equals(role) && !course.getTeacherId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    private void checkLessonOwner(Lesson lesson) {
        Semester semester = semesterMapper.selectById(lesson.getSemesterId());
        if (semester == null) {
            throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        }
        checkSemesterOwner(semester);
    }
}
