package com.example.edu.modules.course.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.course.dto.CourseResourceCreateDTO;
import com.example.edu.modules.course.dto.CourseResourceMoveDTO;
import com.example.edu.modules.course.dto.CourseResourceUpdateDTO;
import com.example.edu.modules.course.service.CourseResourceService;
import com.example.edu.modules.course.vo.CourseResourceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课程资源", description = "课程资源文件夹管理（树形结构）")
@RestController
@RequiredArgsConstructor
public class CourseResourceController {

    private final CourseResourceService courseResourceService;

    @Operation(summary = "创建资源文件夹")
    @PostMapping("/api/courses/{courseId}/resources")
    @PreAuthorize("hasRole('TEACHER')")
    public R<CourseResourceVO> createFolder(@PathVariable Long courseId,
                                            @Valid @RequestBody CourseResourceCreateDTO dto) {
        return R.ok(courseResourceService.createFolder(courseId, dto));
    }

    @Operation(summary = "重命名资源")
    @PutMapping("/api/resources/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public R<Void> rename(@PathVariable Long id,
                          @Valid @RequestBody CourseResourceUpdateDTO dto) {
        courseResourceService.rename(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除资源（级联删除子节点）")
    @DeleteMapping("/api/resources/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        courseResourceService.delete(id);
        return R.ok();
    }

    @Operation(summary = "移动资源")
    @PutMapping("/api/resources/{id}/move")
    @PreAuthorize("hasRole('TEACHER')")
    public R<Void> move(@PathVariable Long id,
                        @Valid @RequestBody CourseResourceMoveDTO dto) {
        courseResourceService.move(id, dto);
        return R.ok();
    }

    @Operation(summary = "获取课程资源树")
    @GetMapping("/api/courses/{courseId}/resources/tree")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public R<List<CourseResourceVO>> getTree(@PathVariable Long courseId) {
        return R.ok(courseResourceService.getTree(courseId));
    }

    @Operation(summary = "获取子资源列表")
    @GetMapping("/api/courses/{courseId}/resources")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public R<List<CourseResourceVO>> getChildren(@PathVariable Long courseId,
                                                  @RequestParam(required = false) Long parentId) {
        return R.ok(courseResourceService.getChildren(courseId, parentId));
    }
}
