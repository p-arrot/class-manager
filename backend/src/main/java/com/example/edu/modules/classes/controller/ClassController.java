package com.example.edu.modules.classes.controller;

import com.example.edu.common.result.PageResult;
import com.example.edu.common.result.R;
import com.example.edu.modules.classes.dto.ClassCreateDTO;
import com.example.edu.modules.classes.dto.ClassUpdateDTO;
import com.example.edu.modules.classes.dto.ClassPageDTO;
import com.example.edu.modules.classes.service.ClassService;
import com.example.edu.modules.classes.vo.ClassVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "班级管理", description = "班级的增删改查")
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @Operation(summary = "创建班级")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<ClassVO> create(@Valid @RequestBody ClassCreateDTO dto) {
        return R.ok(classService.create(dto));
    }

    @Operation(summary = "删除班级")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return R.ok();
    }

    @Operation(summary = "更新班级")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<ClassVO> update(@PathVariable Long id, @Valid @RequestBody ClassUpdateDTO dto) {
        return R.ok(classService.update(id, dto));
    }

    @Operation(summary = "获取班级详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<ClassVO> getById(@PathVariable Long id) {
        return R.ok(classService.getById(id));
    }

    @Operation(summary = "分页查询班级列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<ClassVO>> page(ClassPageDTO dto) {
        return R.ok(PageResult.of(classService.page(dto)));
    }

    @Operation(summary = "获取全部班级列表（下拉选择用）")
    @GetMapping("/list-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<List<ClassVO>> listAll() {
        return R.ok(classService.listAll());
    }
}
