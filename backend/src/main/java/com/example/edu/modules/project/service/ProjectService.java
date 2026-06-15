package com.example.edu.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.classes.entity.SchoolClass;
import com.example.edu.modules.classes.mapper.SchoolClassMapper;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.evaluation.entity.DimensionScore;
import com.example.edu.modules.evaluation.mapper.DimensionScoreMapper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.project.entity.*;
import com.example.edu.modules.project.mapper.*;
import com.example.edu.modules.project.vo.ProjectSubmissionVO;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
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
    private final ProjectTeamMapper teamMapper;
    private final ProjectTeamMemberMapper teamMemberMapper;
    private final ProjectSubmissionMapper submissionMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final UserMapper userMapper;
    private final DimensionScoreMapper dimensionScoreMapper;
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

    @Transactional(rollbackFor = Exception.class)
    public Project update(Long id, Project project) {
        Project existing = projectMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkProjectTeacherAccess(existing);
        existing.setName(project.getName());
        existing.setDescription(project.getDescription());
        existing.setMaxTeamSize(project.getMaxTeamSize());
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
    public ProjectTeam createTeam(Long projectId, String name) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectAccess(project);
        String teamName = name == null ? "" : name.trim();
        if (teamName.isEmpty()) throw new BizException(ErrorCode.BAD_REQUEST, "请输入队伍名称");

        Long studentId = SecurityUtils.getCurrentUserId();
        ProjectTeam team = new ProjectTeam();
        team.setProjectId(projectId);
        team.setName(teamName);
        teamMapper.insert(team);

        ProjectTeamMember member = new ProjectTeamMember();
        member.setTeamId(team.getId());
        member.setStudentId(studentId);
        teamMemberMapper.insert(member);

        auditLogService.record("创建项目队伍", "project_team", team.getId(), teamName);
        return team;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectTeam joinTeam(Long teamId) {
        ProjectTeam team = teamMapper.selectById(teamId);
        if (team == null) throw new BizException(ErrorCode.NOT_FOUND, "队伍不存在");

        Project project = projectMapper.selectById(team.getProjectId());
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectAccess(project);

        Long memberCount = teamMemberMapper.selectCount(new LambdaQueryWrapper<ProjectTeamMember>()
                .eq(ProjectTeamMember::getTeamId, teamId));
        if (project.getMaxTeamSize() != null && memberCount >= project.getMaxTeamSize()) {
            throw new BizException(ErrorCode.CONFLICT, "队伍人数已满");
        }

        Long studentId = SecurityUtils.getCurrentUserId();
        Long existing = teamMemberMapper.selectCount(new LambdaQueryWrapper<ProjectTeamMember>()
                .eq(ProjectTeamMember::getTeamId, teamId)
                .eq(ProjectTeamMember::getStudentId, studentId));
        if (existing > 0) return team;

        ProjectTeamMember member = new ProjectTeamMember();
        member.setTeamId(teamId);
        member.setStudentId(studentId);
        teamMemberMapper.insert(member);

        auditLogService.record("加入项目队伍", "project_team", teamId, team.getName());
        return team;
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
        sub.setContent(content);
        sub.setSubmittedAt(LocalDateTime.now());
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
        List<User> students = getCourseStudents(course.getId());
        List<ProjectSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ProjectSubmission>()
                .eq(ProjectSubmission::getProjectId, projectId)
                .orderByDesc(ProjectSubmission::getSubmittedAt));
        return buildSubmissionInbox(projectId, students, submissions);
    }

    @Transactional(rollbackFor = Exception.class)
    public void scoreSubmission(Long submissionId, List<DimensionScoreService.ScoreInput> scores) {
        ProjectSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Project project = projectMapper.selectById(sub.getProjectId());
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        checkProjectTeacherAccess(project);
        dimensionScoreService.replaceScores("project", submissionId, sub.getStudentId(), scores);
        auditLogService.record("项目作品评分", "project_submission", submissionId, project.getName());
    }

    @Transactional
    public void score(Long projectId, List<ProjectScore> scores) {
        throw new BizException(ErrorCode.BAD_REQUEST, "旧项目评分接口已停用，请使用项目提交逐维度评分接口");
    }

    public List<ProjectScore> listScores(Long projectId) {
        throw new BizException(ErrorCode.BAD_REQUEST, "旧项目评分查询接口已停用，请查看项目提交逐维度评分");
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

    private List<User> getCourseStudents(Long courseId) {
        List<CourseClass> bindings = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, courseId));
        Set<Long> classIds = bindings.stream()
                .map(CourseClass::getClassId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (classIds.isEmpty()) return List.of();
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student")
                .in(User::getClassId, classIds));
    }

    private List<ProjectSubmissionVO> buildSubmissionInbox(Long projectId, List<User> students, List<ProjectSubmission> submissions) {
        Map<Long, ProjectSubmission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(ProjectSubmission::getStudentId, sub -> sub, (left, right) -> left));
        Map<Long, SchoolClass> classMap = loadClassMap(students);
        Map<Long, BigDecimal> scoreMap = loadProjectScoreMap(submissions);
        return students.stream()
                .sorted(Comparator
                        .comparing((User user) -> Optional.ofNullable(formatClassName(classMap.get(user.getClassId()))).orElse(""))
                        .thenComparing(user -> Optional.ofNullable(user.getStudentNo()).orElse(""))
                        .thenComparing(user -> Optional.ofNullable(user.getName()).orElse("")))
                .map(student -> {
                    ProjectSubmission submission = submissionMap.get(student.getId());
                    BigDecimal score = submission != null ? scoreMap.get(submission.getId()) : null;
                    SchoolClass schoolClass = student.getClassId() != null ? classMap.get(student.getClassId()) : null;
                    return ProjectSubmissionVO.builder()
                            .id(submission != null ? submission.getId() : null)
                            .submissionId(submission != null ? submission.getId() : null)
                            .projectId(projectId)
                            .teamId(submission != null ? submission.getTeamId() : null)
                            .studentId(student.getId())
                            .studentName(student.getName())
                            .studentNo(student.getStudentNo())
                            .classId(student.getClassId())
                            .className(formatClassName(schoolClass))
                            .content(submission != null ? submission.getContent() : null)
                            .status(submission == null ? "not_submitted" : score != null ? "graded" : "submitted")
                            .score(score)
                            .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                            .createdAt(submission != null ? submission.getCreatedAt() : null)
                            .build();
                })
                .toList();
    }

    private Map<Long, BigDecimal> loadProjectScoreMap(List<ProjectSubmission> submissions) {
        List<Long> submissionIds = submissions.stream()
                .map(ProjectSubmission::getId)
                .filter(Objects::nonNull)
                .toList();
        if (submissionIds.isEmpty()) return Map.of();
        List<DimensionScore> scores = dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getSourceType, "project")
                .in(DimensionScore::getSourceId, submissionIds));
        return scores.stream()
                .collect(Collectors.groupingBy(
                        DimensionScore::getSourceId,
                        Collectors.reducing(BigDecimal.ZERO,
                                score -> Optional.ofNullable(score.getEarnedScore()).orElse(BigDecimal.ZERO),
                                BigDecimal::add)));
    }

    private Map<Long, SchoolClass> loadClassMap(List<User> students) {
        Set<Long> classIds = students.stream()
                .map(User::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (classIds.isEmpty()) return Map.of();
        return Optional.ofNullable(schoolClassMapper.selectBatchIds(classIds)).orElse(List.of()).stream()
                .collect(Collectors.toMap(SchoolClass::getId, schoolClass -> schoolClass));
    }

    private String formatClassName(SchoolClass schoolClass) {
        if (schoolClass == null) return null;
        return Optional.ofNullable(schoolClass.getGrade()).orElse("")
                + "级"
                + Optional.ofNullable(schoolClass.getName()).orElse("");
    }
}
