package com.example.edu.modules.user.controller;

import com.example.edu.common.result.PageResult;
import com.example.edu.common.result.R;
import com.example.edu.modules.user.dto.*;
import com.example.edu.modules.user.service.TeacherService;
import com.example.edu.modules.user.vo.TeacherClassVO;
import com.example.edu.modules.user.vo.TeacherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "教师管理", description = "教师账号增删改查与班级绑定")
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "创建教师")
    @PostMapping
    public R<TeacherVO> create(@Valid @RequestBody TeacherCreateDTO dto) {
        return R.ok(teacherService.create(dto));
    }

    @Operation(summary = "更新教师")
    @PutMapping("/{id}")
    public R<TeacherVO> update(@PathVariable Long id, @Valid @RequestBody TeacherUpdateDTO dto) {
        return R.ok(teacherService.update(id, dto));
    }

    @Operation(summary = "分页查询教师列表")
    @GetMapping
    public R<PageResult<TeacherVO>> page(TeacherPageDTO dto) {
        return R.ok(PageResult.of(teacherService.page(dto)));
    }

    @Operation(summary = "获取教师详情")
    @GetMapping("/{id}")
    public R<TeacherVO> getById(@PathVariable Long id) {
        return R.ok(teacherService.getById(id));
    }

    @Operation(summary = "获取教师负责的班级列表")
    @GetMapping("/{id}/classes")
    public R<List<TeacherClassVO>> getTeacherClasses(@PathVariable Long id) {
        return R.ok(teacherService.getTeacherClasses(id));
    }

    @Operation(summary = "批量绑定班级")
    @PostMapping("/{id}/classes")
    public R<Integer> batchBind(@PathVariable Long id, @Valid @RequestBody BatchBindDTO dto) {
        return R.ok(teacherService.batchBind(id, dto));
    }

    @Operation(summary = "批量解绑班级")
    @DeleteMapping("/{id}/classes")
    public R<Integer> batchUnbind(@PathVariable Long id, @Valid @RequestBody BatchUnbindDTO dto) {
        return R.ok(teacherService.batchUnbind(id, dto));
    }

    @Operation(summary = "删除教师")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return R.ok();
    }

    @Operation(summary = "重置教师密码")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordResetDTO dto) {
        teacherService.resetPassword(id, dto);
        return R.ok();
    }
}
