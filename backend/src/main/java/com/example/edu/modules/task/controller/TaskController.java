package com.example.edu.modules.task.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.task.dto.SubmissionDTO;
import com.example.edu.modules.task.dto.TaskCreateDTO;
import com.example.edu.modules.task.dto.TaskUpdateDTO;
import com.example.edu.modules.task.service.TaskService;
import com.example.edu.modules.task.vo.SubmissionVO;
import com.example.edu.modules.task.vo.TaskDetailVO;
import com.example.edu.modules.task.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课堂任务")
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "获取课时下的任务列表")
    @GetMapping("/api/lessons/{lessonId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<TaskVO>> listByLesson(@PathVariable Long lessonId) {
        return R.ok(taskService.listByLessonId(lessonId));
    }

    @Operation(summary = "创建任务")
    @PostMapping("/api/lessons/{lessonId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<TaskVO> create(@PathVariable Long lessonId,
                            @Valid @RequestBody TaskCreateDTO dto) {
        return R.ok(taskService.create(lessonId, dto));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/api/tasks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<TaskDetailVO> getById(@PathVariable Long id) {
        return R.ok(taskService.getById(id));
    }

    @Operation(summary = "编辑任务")
    @PutMapping("/api/tasks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<TaskVO> update(@PathVariable Long id,
                            @Valid @RequestBody TaskUpdateDTO dto) {
        return R.ok(taskService.update(id, dto));
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/api/tasks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    @Operation(summary = "学生提交任务")
    @PostMapping("/api/tasks/{taskId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public R<SubmissionVO> submit(@PathVariable Long taskId,
                                  @Valid @RequestBody SubmissionDTO dto) {
        return R.ok(taskService.submit(taskId, dto));
    }

    @Operation(summary = "查看单个提交详情")
    @GetMapping("/api/submissions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<SubmissionVO> getSubmission(@PathVariable Long id) {
        return R.ok(taskService.getSubmission(id));
    }

    @Operation(summary = "教师查看任务提交列表")
    @GetMapping("/api/tasks/{taskId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<SubmissionVO>> listSubmissions(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long classId) {
        return R.ok(taskService.listSubmissions(taskId, classId));
    }
}
