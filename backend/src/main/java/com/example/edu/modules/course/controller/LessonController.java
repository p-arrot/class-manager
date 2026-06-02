package com.example.edu.modules.course.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.course.dto.LessonCreateDTO;
import com.example.edu.modules.course.dto.LessonSortDTO;
import com.example.edu.modules.course.dto.LessonUpdateDTO;
import com.example.edu.modules.course.service.LessonService;
import com.example.edu.modules.course.vo.LessonVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课时管理", description = "课时的增删改查与排序")
@RestController
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @Operation(summary = "创建课时")
    @PostMapping("/api/semesters/{semesterId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<LessonVO> create(@PathVariable Long semesterId,
                              @Valid @RequestBody LessonCreateDTO dto) {
        return R.ok(lessonService.create(semesterId, dto));
    }

    @Operation(summary = "删除课时")
    @DeleteMapping("/api/lessons/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return R.ok();
    }

    @Operation(summary = "更新课时")
    @PutMapping("/api/lessons/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<LessonVO> update(@PathVariable Long id,
                              @Valid @RequestBody LessonUpdateDTO dto) {
        return R.ok(lessonService.update(id, dto));
    }

    @Operation(summary = "调整课时顺序")
    @PutMapping("/api/lessons/{id}/sort")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> reorder(@PathVariable Long id,
                           @Valid @RequestBody LessonSortDTO dto) {
        lessonService.reorder(id, dto);
        return R.ok();
    }

    @Operation(summary = "获取课时详情")
    @GetMapping("/api/lessons/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<LessonVO> getById(@PathVariable Long id) {
        return R.ok(lessonService.getById(id));
    }

    @Operation(summary = "获取学期下的课时列表")
    @GetMapping("/api/semesters/{semesterId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<LessonVO>> listBySemesterId(@PathVariable Long semesterId) {
        return R.ok(lessonService.listBySemesterId(semesterId));
    }
}
