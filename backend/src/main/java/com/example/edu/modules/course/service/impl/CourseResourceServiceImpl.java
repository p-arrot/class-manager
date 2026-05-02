package com.example.edu.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.dto.CourseResourceCreateDTO;
import com.example.edu.modules.course.dto.CourseResourceMoveDTO;
import com.example.edu.modules.course.dto.CourseResourceUpdateDTO;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.CourseResource;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.CourseResourceMapper;
import com.example.edu.modules.course.service.CourseResourceService;
import com.example.edu.modules.course.vo.CourseResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseResourceServiceImpl implements CourseResourceService {

    private final CourseResourceMapper courseResourceMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseResourceVO createFolder(Long courseId, CourseResourceCreateDTO dto) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkTeacherOwnsCourse(course);

        // Validate parent exists and belongs to same course
        if (dto.getParentId() != null) {
            CourseResource parent = courseResourceMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getCourseId().equals(courseId)) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        // Calculate next sort order among siblings
        List<CourseResource> siblings = courseResourceMapper.selectList(
                new LambdaQueryWrapper<CourseResource>()
                        .eq(dto.getParentId() != null,
                                CourseResource::getParentId, dto.getParentId())
                        .isNull(dto.getParentId() == null,
                                CourseResource::getParentId)
                        .eq(CourseResource::getCourseId, courseId)
                        .orderByDesc(CourseResource::getSortOrder)
                        .last("LIMIT 1"));
        int nextSortOrder = siblings.isEmpty() ? 1 : siblings.get(0).getSortOrder() + 1;

        CourseResource resource = new CourseResource();
        resource.setCourseId(courseId);
        resource.setParentId(dto.getParentId());
        resource.setName(dto.getName());
        resource.setType("FOLDER");
        resource.setSortOrder(nextSortOrder);
        courseResourceMapper.insert(resource);

        auditLogService.record("创建资源文件夹", "course_resource", resource.getId(), resource.getName());
        return toVO(resource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, CourseResourceUpdateDTO dto) {
        CourseResource resource = courseResourceMapper.selectById(id);
        if (resource == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        checkResourceOwner(resource);

        resource.setName(dto.getName());
        courseResourceMapper.updateById(resource);

        auditLogService.record("重命名资源", "course_resource", id,
                resource.getName() + " -> " + dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CourseResource resource = courseResourceMapper.selectById(id);
        if (resource == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        checkResourceOwner(resource);

        // Recursively collect and delete all descendants
        List<Long> idsToDelete = new ArrayList<>();
        collectDescendantIds(id, idsToDelete);
        idsToDelete.add(id);

        for (Long rid : idsToDelete) {
            courseResourceMapper.deleteById(rid);
        }

        auditLogService.record("删除资源文件夹", "course_resource", id, resource.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, CourseResourceMoveDTO dto) {
        CourseResource resource = courseResourceMapper.selectById(id);
        if (resource == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        checkResourceOwner(resource);

        // Validate target parent
        if (dto.getTargetParentId() != null) {
            if (dto.getTargetParentId().equals(id)) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "不能移动到自身");
            }
            CourseResource targetParent = courseResourceMapper.selectById(dto.getTargetParentId());
            if (targetParent == null || !targetParent.getCourseId().equals(resource.getCourseId())) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            // Prevent moving into own descendant
            List<Long> descendants = new ArrayList<>();
            collectDescendantIds(id, descendants);
            if (descendants.contains(dto.getTargetParentId())) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "不能移动到子文件夹中");
            }
        }

        resource.setParentId(dto.getTargetParentId());

        // Handle sort order
        if (dto.getTargetSortOrder() != null) {
            resource.setSortOrder(dto.getTargetSortOrder());
        } else {
            // Append to end
            List<CourseResource> siblings = courseResourceMapper.selectList(
                    new LambdaQueryWrapper<CourseResource>()
                            .eq(dto.getTargetParentId() != null,
                                    CourseResource::getParentId, dto.getTargetParentId())
                            .isNull(dto.getTargetParentId() == null,
                                    CourseResource::getParentId)
                            .eq(CourseResource::getCourseId, resource.getCourseId())
                            .orderByDesc(CourseResource::getSortOrder)
                            .last("LIMIT 1"));
            resource.setSortOrder(siblings.isEmpty() ? 1 : siblings.get(0).getSortOrder() + 1);
        }

        courseResourceMapper.updateById(resource);
    }

    @Override
    public List<CourseResourceVO> getTree(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkCourseAccess(course);

        List<CourseResource> all = courseResourceMapper.selectList(
                new LambdaQueryWrapper<CourseResource>()
                        .eq(CourseResource::getCourseId, courseId)
                        .orderByAsc(CourseResource::getSortOrder));

        Map<Long, List<CourseResource>> byParent = all.stream()
                .collect(Collectors.groupingBy(r ->
                        r.getParentId() != null ? r.getParentId() : 0L));

        return buildTree(byParent, 0L);
    }

    @Override
    public List<CourseResourceVO> getChildren(Long courseId, Long parentId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkCourseAccess(course);

        List<CourseResource> resources = courseResourceMapper.selectList(
                new LambdaQueryWrapper<CourseResource>()
                        .eq(CourseResource::getCourseId, courseId)
                        .eq(parentId != null, CourseResource::getParentId, parentId)
                        .isNull(parentId == null, CourseResource::getParentId)
                        .orderByAsc(CourseResource::getSortOrder));
        return resources.stream().map(this::toVO).toList();
    }

    // ========== private helpers ==========

    private CourseResourceVO toVO(CourseResource resource) {
        return CourseResourceVO.builder()
                .id(resource.getId())
                .name(resource.getName())
                .courseId(resource.getCourseId())
                .parentId(resource.getParentId())
                .type(resource.getType())
                .sortOrder(resource.getSortOrder())
                .children(List.of())
                .createdAt(resource.getCreatedAt())
                .build();
    }

    private List<CourseResourceVO> buildTree(Map<Long, List<CourseResource>> byParent, Long parentKey) {
        List<CourseResource> children = byParent.getOrDefault(parentKey, List.of());
        return children.stream()
                .map(c -> {
                    CourseResourceVO vo = toVO(c);
                    List<CourseResourceVO> childNodes = buildTree(byParent, c.getId());
                    vo.setChildren(childNodes);
                    return vo;
                })
                .toList();
    }

    private void collectDescendantIds(Long parentId, List<Long> result) {
        List<CourseResource> children = courseResourceMapper.selectList(
                new LambdaQueryWrapper<CourseResource>()
                        .eq(CourseResource::getParentId, parentId));
        for (CourseResource child : children) {
            result.add(child.getId());
            collectDescendantIds(child.getId(), result);
        }
    }

    private void checkTeacherOwnsCourse(Course course) {
        String role = SecurityUtils.getCurrentUserRole();
        if (!"admin".equals(role) && !course.getTeacherId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    private void checkResourceOwner(CourseResource resource) {
        Course course = courseMapper.selectById(resource.getCourseId());
        if (course == null) {
            throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        }
        checkTeacherOwnsCourse(course);
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
            }
        }
    }
}
