package com.example.edu.modules.user.controller;

import com.example.edu.common.result.PageResult;
import com.example.edu.common.result.R;
import com.example.edu.modules.user.dto.PasswordResetDTO;
import com.example.edu.modules.user.dto.StudentBatchIdsDTO;
import com.example.edu.modules.user.dto.StudentCreateDTO;
import com.example.edu.modules.user.dto.StudentPageDTO;
import com.example.edu.modules.user.dto.StudentUpdateDTO;
import com.example.edu.modules.user.service.StudentService;
import com.example.edu.modules.user.vo.StudentImportResultVO;
import com.example.edu.modules.user.vo.StudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "学生管理", description = "学生CRUD、导入、列表查询、密码重置")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "创建学生")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<StudentVO> create(@Valid @RequestBody StudentCreateDTO dto) {
        return R.ok(studentService.create(dto));
    }

    @Operation(summary = "编辑学生信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<StudentVO> update(@PathVariable Long id, @Valid @RequestBody StudentUpdateDTO dto) {
        return R.ok(studentService.update(id, dto));
    }

    @Operation(summary = "删除学生")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return R.ok();
    }

    @Operation(summary = "导入学生（Excel）")
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<StudentImportResultVO> importStudents(@RequestParam("file") MultipartFile file) {
        return R.ok(studentService.importStudents(file));
    }

    @Operation(summary = "分页查询学生列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<PageResult<StudentVO>> listStudents(StudentPageDTO dto) {
        return R.ok(PageResult.of(studentService.listStudents(dto)));
    }

    @Operation(summary = "重置学生密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetDTO dto) {
        studentService.resetPassword(id, dto);
        return R.ok();
    }

    @Operation(summary = "批量删除学生")
    @PostMapping("/batch/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<Void> batchDelete(@Valid @RequestBody StudentBatchIdsDTO dto) {
        studentService.batchDelete(dto);
        return R.ok();
    }

    @Operation(summary = "批量重置密码")
    @PostMapping("/batch/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<Void> batchResetPassword(@Valid @RequestBody StudentBatchIdsDTO dto) {
        studentService.batchResetPassword(dto);
        return R.ok();
    }
}
