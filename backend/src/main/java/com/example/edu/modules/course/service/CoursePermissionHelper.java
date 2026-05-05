package com.example.edu.modules.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.CourseResource;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;

/**
 * 课程模块权限校验工具类，供多个 ServiceImpl 共用
 */
public final class CoursePermissionHelper {

    private CoursePermissionHelper() {}

    public static void checkTeacherOwnsCourse(Course course) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("admin".equals(role)) return;
        if (!course.getTeacherId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    public static void checkResourceOwner(CourseResource resource, CourseMapper courseMapper) {
        Course course = courseMapper.selectById(resource.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        checkTeacherOwnsCourse(course);
    }

    public static void checkCourseAccess(Course course, CourseClassMapper courseClassMapper) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("admin".equals(role)) return;
        if ("teacher".equals(role)) {
            if (!course.getTeacherId().equals(SecurityUtils.getCurrentUserId())) {
                throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
            }
            return;
        }
        if ("student".equals(role)) {
            Long classId = SecurityUtils.getCurrentUserClassId();
            if (classId == null) {
                throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
            }
            Long count = courseClassMapper.selectCount(
                    new LambdaQueryWrapper<CourseClass>()
                            .eq(CourseClass::getCourseId, course.getId())
                            .eq(CourseClass::getClassId, classId));
            if (count == 0) throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
            return;
        }
        throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
    }
}
