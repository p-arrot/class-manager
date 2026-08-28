package com.example.edu.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.common.submission.SubmissionStatus;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.course.service.CourseRosterService;
import com.example.edu.modules.course.service.CourseRosterService.CourseRoster;
import com.example.edu.modules.evaluation.entity.DimensionScore;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.project.entity.*;
import com.example.edu.modules.project.mapper.*;
import com.example.edu.modules.project.vo.ProjectSubmissionVO;
import com.example.edu.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectSubmissionMapper submissionMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final CourseRosterService courseRosterService;
    private final AuditLogService auditLogService;
    private final DimensionScoreService dimensionScoreService;

    @Transactional
    public Project create(Project project) {
        checkSemesterTeacherAccess(project.getSemesterId());
        projectMapper.insert(project);
        auditLogService.record("创建项目", "project", project.getId(), project.getName());
        return project;
    }

    public List<Project> listBySemester(Long semesterId) {
        checkSemesterAccess(semesterId);
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getSemesterId, semesterId)
                .orderByDesc(Project::getCreatedAt));
    }

    public Project getProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectAccess(project);
        return project;
    }

    @Transactional(rollbackFor = Exception.class)
    public Project update(Long id, Project project) {
        Project existing = projectMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkProjectTeacherAccess(existing);
        existing.setName(project.getName());
        existing.setDescription(project.getDescription());
        existing.setDeadline(project.getDeadline());
        existing.setWeight(project.getWeight());
        projectMapper.updateById(existing);
        auditLogService.record("更新项目", "project", id, existing.getName());
        return existing;
    }

    public void delete(Long id) {
        Project p = projectMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkProjectTeacherAccess(p);
        projectMapper.deleteById(id);
        auditLogService.record("删除项目", "project", id, p.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectSubmission submit(Long projectId, String content) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectAccess(project);
        if (project.getDeadline() != null && LocalDateTime.now().isAfter(project.getDeadline())) {
            throw new BizException(ErrorCode.TASK_DEADLINE_PASSED);
        }
        Long studentId = SecurityUtils.getCurrentUserId();
        ProjectSubmission sub = submissionMapper.selectOne(new LambdaQueryWrapper<ProjectSubmission>()
                .eq(ProjectSubmission::getProjectId, projectId)
                .eq(ProjectSubmission::getStudentId, studentId));
        if (sub == null) {
            sub = new ProjectSubmission();
            sub.setProjectId(projectId);
            sub.setStudentId(studentId);
        }
        if (SubmissionStatus.isLocked(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_LOCKED);
        }
        boolean returned = SubmissionStatus.RETURNED.equals(sub.getStatus());
        sub.setContent(content);
        sub.setStatus(SubmissionStatus.SUBMITTED);
        sub.setSubmittedAt(LocalDateTime.now());
        sub.setReturnReason(null);
        sub.setReturnedAt(null);
        if (returned) sub.setRevisionCount(Optional.ofNullable(sub.getRevisionCount()).orElse(0) + 1);
        if (sub.getId() == null) submissionMapper.insert(sub);
        else submissionMapper.updateById(sub);
        auditLogService.record("提交项目作品", "project", projectId, project.getName());
        return sub;
    }

    public List<ProjectSubmission> listSubmissions(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectTeacherAccess(project);
        return submissionMapper.selectList(new LambdaQueryWrapper<ProjectSubmission>()
                .eq(ProjectSubmission::getProjectId, projectId)
                .orderByDesc(ProjectSubmission::getSubmittedAt));
    }

    public List<ProjectSubmissionVO> listSubmissionInbox(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        Course course = checkProjectTeacherAccess(project);
        CourseRoster roster = courseRosterService.load(course.getId());
        List<ProjectSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ProjectSubmission>()
                .eq(ProjectSubmission::getProjectId, projectId)
                .orderByDesc(ProjectSubmission::getSubmittedAt));
        return buildSubmissionInbox(projectId, roster, submissions);
    }

    @Transactional(rollbackFor = Exception.class)
    public void scoreSubmission(Long submissionId, List<DimensionScoreService.ScoreInput> scores) {
        ProjectSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Project project = projectMapper.selectById(sub.getProjectId());
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectTeacherAccess(project);
        if (SubmissionStatus.RETURNED.equals(sub.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "学生尚未重新提交，不能评分");
        }
        dimensionScoreService.replaceScores("project", submissionId, sub.getStudentId(), scores);
        sub.setStatus(SubmissionStatus.GRADED);
        sub.setReturnReason(null);
        sub.setReturnedAt(null);
        submissionMapper.updateById(sub);
        auditLogService.record("项目作品评分", "project_submission", submissionId, project.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void returnSubmission(Long submissionId, String reason) {
        ProjectSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Project project = projectMapper.selectById(sub.getProjectId());
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectTeacherAccess(project);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) throw new BizException(ErrorCode.BAD_REQUEST, "请填写退回原因");
        sub.setStatus(SubmissionStatus.RETURNED);
        sub.setReturnReason(normalizedReason);
        sub.setReturnedAt(LocalDateTime.now());
        submissionMapper.updateById(sub);
        dimensionScoreService.clearScores("project", submissionId);
        auditLogService.record("退回项目修改", "project_submission", submissionId, normalizedReason);
    }

    public Map<Long, ProjectSubmission> listMySubmissions(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty() || !"student".equals(SecurityUtils.getCurrentUserRole())) return Map.of();
        return submissionMapper.selectList(new LambdaQueryWrapper<ProjectSubmission>()
                        .eq(ProjectSubmission::getStudentId, SecurityUtils.getCurrentUserId())
                        .in(ProjectSubmission::getProjectId, projectIds))
                .stream().collect(Collectors.toMap(ProjectSubmission::getProjectId, sub -> sub));
    }

    public ProjectSubmission getMySubmission(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectAccess(project);
        return submissionMapper.selectOne(new LambdaQueryWrapper<ProjectSubmission>()
                .eq(ProjectSubmission::getProjectId, projectId)
                .eq(ProjectSubmission::getStudentId, SecurityUtils.getCurrentUserId()));
    }

    public List<ProjectSubmissionVO.DimensionScoreVO> getSubmissionScoreDetails(Long submissionId) {
        ProjectSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Project project = projectMapper.selectById(submission.getProjectId());
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectAccess(project);
        if ("student".equals(SecurityUtils.getCurrentUserRole())
                && !Objects.equals(submission.getStudentId(), SecurityUtils.getCurrentUserId())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
        return toScoreDetails(dimensionScoreService.listBySources("project", List.of(submissionId)));
    }

    private void checkProjectAccess(Project project) {
        Semester semester = semesterMapper.selectById(project.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
    }

    private Course checkProjectTeacherAccess(Project project) {
        Semester semester = semesterMapper.selectById(project.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
        return course;
    }

    private void checkSemesterAccess(Long semesterId) {
        Semester semester = semesterMapper.selectById(semesterId);
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
    }

    private void checkSemesterTeacherAccess(Long semesterId) {
        Semester semester = semesterMapper.selectById(semesterId);
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
    }

    private List<ProjectSubmissionVO> buildSubmissionInbox(Long projectId, CourseRoster roster, List<ProjectSubmission> submissions) {
        Map<Long, ProjectSubmission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(ProjectSubmission::getStudentId, sub -> sub, (left, right) -> left));
        Map<Long, List<ProjectSubmissionVO.DimensionScoreVO>> scoreMap = loadProjectScoreMap(submissions);
        return roster.students().stream()
                .map(student -> {
                    ProjectSubmission submission = submissionMap.get(student.getId());
                    List<ProjectSubmissionVO.DimensionScoreVO> scoreDetails = submission != null
                            ? scoreMap.getOrDefault(submission.getId(), List.of()) : List.of();
                    BigDecimal score = totalScore(scoreDetails);
                    return ProjectSubmissionVO.builder()
                            .id(submission != null ? submission.getId() : null)
                            .submissionId(submission != null ? submission.getId() : null)
                            .projectId(projectId)
                            .studentId(student.getId())
                            .studentName(student.getName())
                            .studentNo(student.getStudentNo())
                            .classId(student.getClassId())
                            .className(roster.displayClassName(student.getClassId()))
                            .content(submission != null ? submission.getContent() : null)
                            .status(submission == null ? "not_submitted"
                                    : submission.getStatus() != null ? submission.getStatus()
                                    : score != null ? SubmissionStatus.GRADED : SubmissionStatus.SUBMITTED)
                            .canResubmit(submission != null && SubmissionStatus.canResubmit(submission.getStatus()))
                            .returnReason(submission != null ? submission.getReturnReason() : null)
                            .returnedAt(submission != null ? submission.getReturnedAt() : null)
                            .revisionCount(submission != null ? Optional.ofNullable(submission.getRevisionCount()).orElse(0) : 0)
                            .score(score)
                            .dimensionScores(scoreDetails)
                            .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                            .createdAt(submission != null ? submission.getCreatedAt() : null)
                            .build();
                })
                .toList();
    }

    private Map<Long, List<ProjectSubmissionVO.DimensionScoreVO>> loadProjectScoreMap(List<ProjectSubmission> submissions) {
        List<Long> submissionIds = submissions.stream()
                .map(ProjectSubmission::getId)
                .filter(Objects::nonNull)
                .toList();
        if (submissionIds.isEmpty()) return Map.of();
        return dimensionScoreService.listBySources("project", submissionIds).stream()
                .collect(Collectors.groupingBy(
                        DimensionScore::getSourceId,
                        Collectors.mapping(this::toScoreDetail, Collectors.toList())));
    }

    private List<ProjectSubmissionVO.DimensionScoreVO> toScoreDetails(List<DimensionScore> scores) {
        return scores.stream().map(this::toScoreDetail).toList();
    }

    private ProjectSubmissionVO.DimensionScoreVO toScoreDetail(DimensionScore score) {
        return new ProjectSubmissionVO.DimensionScoreVO(
                score.getQuestionId(),
                score.getDimension(),
                score.getEarnedScore(),
                score.getMaxScore());
    }

    private BigDecimal totalScore(List<ProjectSubmissionVO.DimensionScoreVO> scores) {
        if (scores == null || scores.isEmpty()) return null;
        return scores.stream()
                .map(ProjectSubmissionVO.DimensionScoreVO::earnedScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
