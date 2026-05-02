package com.example.edu.modules.course.controller;

import com.example.edu.common.result.PageResult;
import com.example.edu.common.result.R;
import com.example.edu.modules.course.dto.CourseCreateDTO;
import com.example.edu.modules.course.dto.CoursePageDTO;
import com.example.edu.modules.course.dto.CourseUpdateDTO;
import com.example.edu.modules.course.service.CourseService;
import com.example.edu.modules.course.vo.CourseDetailVO;
import com.example.edu.modules.course.vo.CourseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "课程管理", description = "课程的增删改查")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "创建课程")
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public R<CourseVO> create(@Valid @RequestBody CourseCreateDTO dto) {
        return R.ok(courseService.create(dto));
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return R.ok();
    }

    @Operation(summary = "更新课程")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public R<CourseVO> update(@PathVariable Long id, @Valid @RequestBody CourseUpdateDTO dto) {
        return R.ok(courseService.update(id, dto));
    }

    @Operation(summary = "获取课程详情（含学期列表和班级绑定）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<CourseDetailVO> getById(@PathVariable Long id) {
        return R.ok(courseService.getById(id));
    }

    @Operation(summary = "分页查询课程列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<PageResult<CourseVO>> page(CoursePageDTO dto) {
        return R.ok(PageResult.of(courseService.page(dto)));
    }
}
