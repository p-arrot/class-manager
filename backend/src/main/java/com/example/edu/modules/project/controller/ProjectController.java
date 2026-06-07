package com.example.edu.modules.project.controller;

import com.example.edu.common.result.R;
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
        return R.ok(projectService.listBySemester(semesterId).stream().map(p -> ProjectVO.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .semesterId(p.getSemesterId()).maxTeamSize(p.getMaxTeamSize())
                .deadline(p.getDeadline()).weight(p.getWeight()).createdAt(p.getCreatedAt()).build()).toList());
    }

    @PostMapping("/api/semesters/{semesterId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ProjectVO> create(@PathVariable Long semesterId, @RequestBody Project project) {
        project.setSemesterId(semesterId);
        Project p = projectService.create(project);
        return R.ok(ProjectVO.builder().id(p.getId()).name(p.getName()).description(p.getDescription())
                .semesterId(p.getSemesterId()).maxTeamSize(p.getMaxTeamSize())
                .deadline(p.getDeadline()).weight(p.getWeight()).createdAt(p.getCreatedAt()).build());
    }

    @PutMapping("/api/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ProjectVO> update(@PathVariable Long id, @RequestBody Project project) {
        Project p = projectService.update(id, project);
        return R.ok(ProjectVO.builder().id(p.getId()).name(p.getName()).description(p.getDescription())
                .semesterId(p.getSemesterId()).maxTeamSize(p.getMaxTeamSize())
                .deadline(p.getDeadline()).weight(p.getWeight()).createdAt(p.getCreatedAt()).build());
    }

    @DeleteMapping("/api/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> delete(@PathVariable Long id) { projectService.delete(id); return R.ok(); }

    @PostMapping("/api/projects/{projectId}/teams")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public R<Map<String,Object>> createTeam(@PathVariable Long projectId, @RequestBody Map<String,String> body) {
        ProjectTeam team = projectService.createTeam(projectId, body.get("name"));
        return R.ok(Map.of("id", team.getId(), "projectId", projectId, "teamName", team.getName(), "status","created"));
    }

    @PostMapping("/api/teams/{teamId}/join")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public R<Map<String,Object>> joinTeam(@PathVariable Long teamId) {
        ProjectTeam team = projectService.joinTeam(teamId);
        return R.ok(Map.of("teamId", team.getId(), "projectId", team.getProjectId(), "teamName", team.getName(), "status", "joined"));
    }

    @PostMapping("/api/projects/{projectId}/submit")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public R<ProjectSubmissionVO> submit(@PathVariable Long projectId, @RequestBody Map<String,String> body) {
        ProjectSubmission sub = projectService.submit(projectId, body.get("content"));
        User u = userMapper.selectById(sub.getStudentId());
        return R.ok(toSubmissionVO(sub, u));
    }

    @GetMapping("/api/projects/{projectId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ProjectSubmissionVO>> listSubmissions(@PathVariable Long projectId) {
        List<ProjectSubmission> subs = projectService.listSubmissions(projectId);
        Set<Long> sids = subs.stream().map(ProjectSubmission::getStudentId).collect(Collectors.toSet());
        Map<Long,User> um = sids.isEmpty() ? Map.of() : userMapper.selectBatchIds(sids).stream().collect(Collectors.toMap(User::getId,u->u));
        return R.ok(subs.stream().map(s -> toSubmissionVO(s, um.get(s.getStudentId()))).toList());
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

    @PostMapping("/api/projects/{projectId}/scores")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> score(@PathVariable Long projectId, @RequestBody List<ProjectScore> scores) {
        projectService.score(projectId, scores); return R.ok();
    }

    @GetMapping("/api/projects/{projectId}/scores")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ProjectScoreVO>> listScores(@PathVariable Long projectId) {
        List<ProjectScore> scores = projectService.listScores(projectId);
        Set<Long> sids = scores.stream().map(ProjectScore::getStudentId).collect(Collectors.toSet());
        Map<Long,User> um = sids.isEmpty() ? Map.of() : userMapper.selectBatchIds(sids).stream().collect(Collectors.toMap(User::getId,u->u));
        return R.ok(scores.stream().map(s -> {
            User u = um.get(s.getStudentId());
            return ProjectScoreVO.builder().id(s.getId()).projectId(s.getProjectId())
                    .studentId(s.getStudentId()).studentName(u!=null?u.getName():null)
                    .studentNo(u!=null?u.getStudentNo():null).grade(s.getGrade())
                    .isSpecial(s.getIsSpecial()).createdAt(s.getCreatedAt()).build();
        }).toList());
    }

    private ProjectSubmissionVO toSubmissionVO(ProjectSubmission sub, User user) {
        return ProjectSubmissionVO.builder()
                .id(sub.getId())
                .projectId(sub.getProjectId())
                .teamId(sub.getTeamId())
                .studentId(sub.getStudentId())
                .studentName(user != null ? user.getName() : null)
                .studentNo(user != null ? user.getStudentNo() : null)
                .content(sub.getContent())
                .submittedAt(sub.getSubmittedAt())
                .createdAt(sub.getCreatedAt())
                .build();
    }

    public record ProjectSubmissionScoreDTO(
            String questionId,
            String dimension,
            BigDecimal earnedScore,
            BigDecimal maxScore
    ) {}
}
