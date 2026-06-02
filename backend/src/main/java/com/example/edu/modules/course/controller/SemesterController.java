package com.example.edu.modules.course.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.course.dto.SemesterCreateDTO;
import com.example.edu.modules.course.dto.SemesterUpdateDTO;
import com.example.edu.modules.course.service.SemesterService;
import com.example.edu.modules.course.vo.SemesterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "学期管理", description = "学期的增删改查")
@RestController
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @Operation(summary = "创建学期")
    @PostMapping("/api/courses/{courseId}/semesters")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<SemesterVO> create(@PathVariable Long courseId,
                                @Valid @RequestBody SemesterCreateDTO dto) {
        return R.ok(semesterService.create(courseId, dto));
    }

    @Operation(summary = "删除学期")
    @DeleteMapping("/api/semesters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        semesterService.delete(id);
        return R.ok();
    }

    @Operation(summary = "更新学期")
    @PutMapping("/api/semesters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<SemesterVO> update(@PathVariable Long id,
                                @Valid @RequestBody SemesterUpdateDTO dto) {
        return R.ok(semesterService.update(id, dto));
    }

    @Operation(summary = "获取学期详情")
    @GetMapping("/api/semesters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<SemesterVO> getById(@PathVariable Long id) {
        return R.ok(semesterService.getById(id));
    }

    @Operation(summary = "获取课程下的学期列表")
    @GetMapping("/api/courses/{courseId}/semesters")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<SemesterVO>> listByCourseId(@PathVariable Long courseId) {
        return R.ok(semesterService.listByCourseId(courseId));
    }
}
