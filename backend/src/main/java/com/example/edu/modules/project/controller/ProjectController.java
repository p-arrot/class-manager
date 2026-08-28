package com.example.edu.modules.project.controller;

import com.example.edu.common.result.R;
import com.example.edu.common.dto.ReturnSubmissionDTO;
import com.example.edu.common.submission.SubmissionStatus;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.project.entity.*;
import com.example.edu.modules.project.service.ProjectService;
import com.example.edu.modules.project.vo.*;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "项目管理")
@RestController @RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final UserMapper userMapper;

    @GetMapping("/api/semesters/{semesterId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<ProjectVO>> list(@PathVariable Long semesterId) {
        List<Project> projects = projectService.listBySemester(semesterId);
        Map<Long, ProjectSubmission> mySubmissions = projectService.listMySubmissions(projects.stream().map(Project::getId).toList());
        return R.ok(projects.stream().map(p -> toProjectVO(p, mySubmissions.get(p.getId()))).toList());
    }

    @PostMapping("/api/semesters/{semesterId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ProjectVO> create(@PathVariable Long semesterId, @RequestBody Project project) {
        project.setSemesterId(semesterId);
        Project p = projectService.create(project);
        return R.ok(toProjectVO(p, null));
    }

    @GetMapping("/api/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<ProjectVO> get(@PathVariable Long id) {
        Project project = projectService.getProject(id);
        ProjectSubmission submission = projectService.listMySubmissions(List.of(id)).get(id);
        return R.ok(toProjectVO(project, submission));
    }

    @PutMapping("/api/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ProjectVO> update(@PathVariable Long id, @RequestBody Project project) {
        Project p = projectService.update(id, project);
        return R.ok(toProjectVO(p, null));
    }

    @DeleteMapping("/api/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) { projectService.delete(id); return R.ok(); }

    @PostMapping("/api/projects/{projectId}/submit")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public R<ProjectSubmissionVO> submit(@PathVariable Long projectId, @RequestBody Map<String,String> body) {
        ProjectSubmission sub = projectService.submit(projectId, body.get("content"));
        User u = userMapper.selectById(sub.getStudentId());
        return R.ok(toSubmissionVO(sub, u));
    }

    @GetMapping("/api/projects/{projectId}/my-submission")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ProjectSubmissionVO> getMySubmission(@PathVariable Long projectId) {
        ProjectSubmission submission = projectService.getMySubmission(projectId);
        if (submission == null) return R.ok(null);
        return R.ok(toSubmissionVO(submission, userMapper.selectById(submission.getStudentId())));
    }

    @GetMapping("/api/projects/{projectId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ProjectSubmissionVO>> listSubmissions(@PathVariable Long projectId) {
        return R.ok(projectService.listSubmissionInbox(projectId));
    }

    @PostMapping("/api/project-submissions/{submissionId}/score")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> scoreSubmission(@PathVariable Long submissionId, @RequestBody List<ProjectSubmissionScoreDTO> scores) {
        projectService.scoreSubmission(submissionId, scores.stream()
                .map(score -> new DimensionScoreService.ScoreInput(
                        score.questionId(),
                        score.dimension(),
                        score.earnedScore(),
                        score.maxScore(),
                        false
                ))
                .toList());
        return R.ok();
    }

    @PutMapping("/api/project-submissions/{submissionId}/return")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> returnSubmission(@PathVariable Long submissionId, @Valid @RequestBody ReturnSubmissionDTO dto) {
        projectService.returnSubmission(submissionId, dto.reason());
        return R.ok();
    }

    private ProjectSubmissionVO toSubmissionVO(ProjectSubmission sub, User user) {
        List<ProjectSubmissionVO.DimensionScoreVO> scoreDetails = sub.getId() == null
                ? List.of() : projectService.getSubmissionScoreDetails(sub.getId());
        BigDecimal totalScore = scoreDetails.stream()
                .map(ProjectSubmissionVO.DimensionScoreVO::earnedScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ProjectSubmissionVO.builder()
                .id(sub.getId())
                .submissionId(sub.getId())
                .projectId(sub.getProjectId())
                .studentId(sub.getStudentId())
                .studentName(user != null ? user.getName() : null)
                .studentNo(user != null ? user.getStudentNo() : null)
                .content(sub.getContent())
                .status(sub.getStatus())
                .canResubmit(SubmissionStatus.canResubmit(sub.getStatus()))
                .returnReason(sub.getReturnReason())
                .returnedAt(sub.getReturnedAt())
                .revisionCount(Optional.ofNullable(sub.getRevisionCount()).orElse(0))
                .score(scoreDetails.isEmpty() ? null : totalScore)
                .dimensionScores(scoreDetails)
                .submittedAt(sub.getSubmittedAt())
                .createdAt(sub.getCreatedAt())
                .build();
    }

    private ProjectVO toProjectVO(Project project, ProjectSubmission submission) {
        return ProjectVO.builder()
                .id(project.getId()).name(project.getName()).description(project.getDescription())
                .semesterId(project.getSemesterId()).deadline(project.getDeadline())
                .weight(project.getWeight()).createdAt(project.getCreatedAt())
                .submissionId(submission != null ? submission.getId() : null)
                .submissionStatus(submission != null ? submission.getStatus() : "not_submitted")
                .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                .returnReason(submission != null ? submission.getReturnReason() : null)
                .canResubmit(submission == null || SubmissionStatus.canResubmit(submission.getStatus()))
                .build();
    }

    public record ProjectSubmissionScoreDTO(
            String questionId,
            String dimension,
            BigDecimal earnedScore,
            BigDecimal maxScore
    ) {}
}
