package com.example.edu.modules.project.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.project.entity.*;
import com.example.edu.modules.project.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "项目管理")
@RestController @RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping("/api/semesters/{semesterId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<Project>> list(@PathVariable Long semesterId) { return R.ok(projectService.listBySemester(semesterId)); }

    @PostMapping("/api/semesters/{semesterId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Project> create(@PathVariable Long semesterId, @RequestBody Project project) {
        project.setSemesterId(semesterId); return R.ok(projectService.create(project));
    }

    @DeleteMapping("/api/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) { projectService.delete(id); return R.ok(); }

    @PostMapping("/api/projects/{projectId}/scores")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> score(@PathVariable Long projectId, @RequestBody List<ProjectScore> scores) {
        projectService.score(projectId, scores); return R.ok();
    }

    @GetMapping("/api/projects/{projectId}/scores")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ProjectScore>> listScores(@PathVariable Long projectId) { return R.ok(projectService.listScores(projectId)); }
}
