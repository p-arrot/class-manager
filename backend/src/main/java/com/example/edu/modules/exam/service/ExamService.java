package com.example.edu.modules.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.common.submission.SubmissionStatus;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.CourseClass;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.CourseClassMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.course.service.CourseRosterService;
import com.example.edu.modules.course.service.CourseRosterService.CourseRoster;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.QuestionScoreHelper;
import com.example.edu.modules.exam.entity.*;
import com.example.edu.modules.exam.mapper.*;
import com.example.edu.modules.exam.vo.ExamSubmissionVO;
import com.example.edu.modules.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> STUDENT_HIDDEN_FIELDS = Set.of(
            "answer", "answers", "autoGrade", "auto_grade", "correctAnswer", "referenceAnswer",
            "expectedAnswer", "correctOption", "solution");

    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final CourseRosterService courseRosterService;
    private final AuditLogService auditLogService;
    private final DimensionScoreService dimensionScoreService;
    private final QuestionScoreHelper questionScoreHelper;

    // Papers
    public ExamPaper createPaper(ExamPaper paper) {
        paper.setTeacherId(SecurityUtils.getCurrentUserId());
        paperMapper.insert(paper);
        auditLogService.record("创建试卷", "exam_paper", paper.getId(), paper.getTitle());
        return paper;
    }

    public List<ExamPaper> listPapers() {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<ExamPaper>()
                .orderByDesc(ExamPaper::getCreatedAt);
        if (!"admin".equals(SecurityUtils.getCurrentUserRole())) {
            wrapper.eq(ExamPaper::getTeacherId, SecurityUtils.getCurrentUserId());
        }
        return paperMapper.selectList(wrapper);
    }

    public List<ExamPaper> listPaperByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return paperMapper.selectBatchIds(ids);
    }

    // Exams
    @Transactional
    public Exam createExam(Exam exam) {
        checkSemesterAccess(exam.getSemesterId());
        validateExam(exam);
        examMapper.insert(exam);
        auditLogService.record("创建考试", "exam", exam.getId(), exam.getName());
        return exam;
    }

    public List<Exam> listExams(Long semesterId) {
        checkSemesterAccess(semesterId);
        return examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getSemesterId, semesterId)
                .orderByDesc(Exam::getCreatedAt));
    }

    public Exam getExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkSemesterAccess(exam.getSemesterId());
        return exam;
    }

    @Transactional(rollbackFor = Exception.class)
    public Exam updateExam(Long id, Exam exam) {
        Exam existing = examMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkSemesterAccess(existing.getSemesterId());
        exam.setSemesterId(existing.getSemesterId());
        validateExam(exam);
        validateExamTimes(exam);
        ensurePaperAccess(exam.getPaperId());
        existing.setName(exam.getName());
        existing.setPaperId(exam.getPaperId());
        existing.setStartTime(exam.getStartTime());
        existing.setEndTime(exam.getEndTime());
        existing.setWeight(exam.getWeight());
        examMapper.updateById(existing);
        auditLogService.record("更新考试", "exam", id, existing.getName());
        return existing;
    }

    private void validateExam(Exam exam) {
        if (exam.getSemesterId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择所属学期");
        }
        if (exam.getPaperId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择试卷");
        }
        if (paperMapper.selectById(exam.getPaperId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        validateExamTimes(exam);
        ensurePaperAccess(exam.getPaperId());
    }

    private void validateExamTimes(Exam exam) {
        if (exam.getStartTime() == null || exam.getEndTime() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请填写考试起止时间");
        }
        if (!exam.getStartTime().isBefore(exam.getEndTime())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "考试开始时间必须早于结束时间");
        }
        if (exam.getWeight() != null && (exam.getWeight().compareTo(BigDecimal.ZERO) < 0
                || exam.getWeight().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "考试权重必须在 0 到 100 之间");
        }
    }

    private void ensurePaperAccess(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BizException(ErrorCode.NOT_FOUND, "试卷不存在");
        if (!"admin".equals(SecurityUtils.getCurrentUserRole())
                && !Objects.equals(paper.getTeacherId(), SecurityUtils.getCurrentUserId())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkSemesterAccess(exam.getSemesterId());
        submissionMapper.delete(new LambdaQueryWrapper<ExamSubmission>().eq(ExamSubmission::getExamId, id));
        examMapper.deleteById(id);
        auditLogService.record("删除考试", "exam", id, exam.getName());
    }

    // Submissions
    @Transactional
    public ExamSubmission submit(Long examId, String answers) {
        Long studentId = SecurityUtils.getCurrentUserId();
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkStudentCanTakeExam(exam);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()))
            throw new BizException(ErrorCode.BAD_REQUEST, "考试尚未开始");
        if (now.isAfter(exam.getEndTime()))
            throw new BizException(ErrorCode.TASK_DEADLINE_PASSED);

        ExamSubmission sub = submissionMapper.selectOne(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId)
                .eq(ExamSubmission::getStudentId, studentId));
        if (sub == null) {
            sub = new ExamSubmission();
            sub.setExamId(examId);
            sub.setStudentId(studentId);
        }
        if (SubmissionStatus.isLocked(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_LOCKED);
        }
        boolean returned = SubmissionStatus.RETURNED.equals(sub.getStatus());
        sub.setAnswers(answers);
        sub.setStatus(SubmissionStatus.SUBMITTED);
        sub.setSubmittedAt(LocalDateTime.now());
        sub.setReturnReason(null);
        sub.setReturnedAt(null);
        if (sub.getStartedAt() == null) sub.setStartedAt(now);
        if (returned) sub.setRevisionCount(Optional.ofNullable(sub.getRevisionCount()).orElse(0) + 1);
        if (sub.getId() == null) submissionMapper.insert(sub);
        else submissionMapper.updateById(sub);
        autoGradeExam(exam, sub);
        auditLogService.record(returned ? "重新提交考试" : "提交考试",
                "exam_submission", sub.getId(), exam.getName());
        return sub;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmission startExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkStudentCanTakeExam(exam);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "考试尚未开始");
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new BizException(ErrorCode.TASK_DEADLINE_PASSED);
        }
        Long studentId = SecurityUtils.getCurrentUserId();
        ExamSubmission sub = findSubmission(examId, studentId);
        if (sub != null && SubmissionStatus.isLocked(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_LOCKED);
        }
        if (sub == null) {
            sub = new ExamSubmission();
            sub.setExamId(examId);
            sub.setStudentId(studentId);
            sub.setStatus(SubmissionStatus.IN_PROGRESS);
            sub.setStartedAt(now);
            sub.setRevisionCount(0);
            submissionMapper.insert(sub);
            auditLogService.record("开始考试", "exam_submission", sub.getId(), exam.getName());
        } else if (SubmissionStatus.RETURNED.equals(sub.getStatus()) || SubmissionStatus.SUBMITTED.equals(sub.getStatus())) {
            sub.setStatus(SubmissionStatus.IN_PROGRESS);
            submissionMapper.updateById(sub);
        }
        return sub;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmission saveDraft(Long examId, String answers) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkStudentCanTakeExam(exam);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            throw new BizException(ErrorCode.TASK_DEADLINE_PASSED);
        }
        ExamSubmission sub = findSubmission(examId, SecurityUtils.getCurrentUserId());
        if (sub == null) sub = startExam(examId);
        if (SubmissionStatus.isLocked(sub.getStatus())) {
            throw new BizException(ErrorCode.SUBMISSION_LOCKED);
        }
        sub.setAnswers(answers);
        sub.setStatus(SubmissionStatus.IN_PROGRESS);
        submissionMapper.updateById(sub);
        return sub;
    }

    public ExamSubmission getMySubmission(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkStudentCanTakeExam(exam);
        return findSubmission(examId, SecurityUtils.getCurrentUserId());
    }

    public Map<Long, ExamSubmission> listMySubmissions(Collection<Long> examIds) {
        if (examIds == null || examIds.isEmpty() || !"student".equals(SecurityUtils.getCurrentUserRole())) return Map.of();
        return submissionMapper.selectList(new LambdaQueryWrapper<ExamSubmission>()
                        .eq(ExamSubmission::getStudentId, SecurityUtils.getCurrentUserId())
                        .in(ExamSubmission::getExamId, examIds))
                .stream().collect(Collectors.toMap(ExamSubmission::getExamId, sub -> sub));
    }

    public List<ExamSubmission> listSubmissions(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkTeacherOwnsExam(exam);
        return submissionMapper.selectList(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId));
    }

    public List<ExamSubmissionVO> listSubmissionInbox(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        Course course = checkTeacherOwnsExam(exam);
        CourseRoster roster = courseRosterService.load(course.getId());
        List<ExamSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId));
        return buildSubmissionInbox(examId, roster, submissions);
    }

    @Transactional(rollbackFor = Exception.class)
    public void gradeSubmission(Long submissionId, Integer score, boolean absent, List<DimensionScoreService.ScoreInput> dimensionScores) {
        ExamSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Exam exam = examMapper.selectById(sub.getExamId());
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkTeacherOwnsExam(exam);
        if (SubmissionStatus.RETURNED.equals(sub.getStatus()) || SubmissionStatus.IN_PROGRESS.equals(sub.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "学生尚未正式提交，不能批改");
        }

        sub.setScore(absent ? 0 : score);
        sub.setStatus(absent ? SubmissionStatus.ABSENT : SubmissionStatus.GRADED);
        sub.setReturnReason(null);
        sub.setReturnedAt(null);
        submissionMapper.updateById(sub);
        if (absent) {
            dimensionScoreService.replaceScores("exam", submissionId, sub.getStudentId(), List.of());
        } else if (dimensionScores != null && !dimensionScores.isEmpty()) {
            dimensionScoreService.replaceScores("exam", submissionId, sub.getStudentId(), dimensionScores);
        }
        auditLogService.record("考试评分", "exam_submission", submissionId, String.valueOf(score));
    }

    @Transactional(rollbackFor = Exception.class)
    public void returnSubmission(Long submissionId, String reason) {
        ExamSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Exam exam = examMapper.selectById(sub.getExamId());
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkTeacherOwnsExam(exam);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) throw new BizException(ErrorCode.BAD_REQUEST, "请填写退回原因");
        sub.setStatus(SubmissionStatus.RETURNED);
        sub.setScore(null);
        sub.setReturnReason(normalizedReason);
        sub.setReturnedAt(LocalDateTime.now());
        submissionMapper.updateById(sub);
        dimensionScoreService.clearScores("exam", submissionId);
        auditLogService.record("退回考试修改", "exam_submission", submissionId, normalizedReason);
    }

    private ExamSubmission findSubmission(Long examId, Long studentId) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId)
                .eq(ExamSubmission::getStudentId, studentId));
    }

    public void gradeSubmission(Long submissionId, Integer score, boolean absent) {
        gradeSubmission(submissionId, score, absent, List.of());
    }

    private void autoGradeExam(Exam exam, ExamSubmission sub) {
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null || paper.getContent() == null || sub.getAnswers() == null) return;
        try {
            List<DimensionScoreService.ScoreInput> scores = questionScoreHelper.autoGrade(paper.getContent(), sub.getAnswers());
            if (scores.isEmpty()) return;
            dimensionScoreService.replaceAutoScores("exam", sub.getId(), sub.getStudentId(), scores);
            int earned = scores.stream().map(DimensionScoreService.ScoreInput::earnedScore)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add).intValue();
            sub.setScore(earned);
            sub.setStatus("graded");
            submissionMapper.updateById(sub);
        } catch (Exception e) {
            log.warn("Exam auto grade skipped: examId={}, submissionId={}", exam.getId(), sub.getId(), e);
        }
    }

    private Course checkTeacherOwnsExam(Exam exam) {
        Course course = getCourseForSemester(exam.getSemesterId());
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
        return course;
    }

    private Course checkSemesterAccess(Long semesterId) {
        Course course = getCourseForSemester(semesterId);
        String role = SecurityUtils.getCurrentUserRole();
        if ("student".equals(role)) {
            CoursePermissionHelper.checkCourseAccess(course, courseClassMapper);
        } else {
            CoursePermissionHelper.checkTeacherOwnsCourse(course);
        }
        return course;
    }

    private Course getCourseForSemester(Long semesterId) {
        Semester semester = semesterMapper.selectById(semesterId);
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        return course;
    }

    private void checkStudentCanTakeExam(Exam exam) {
        Course course = checkSemesterAccess(exam.getSemesterId());
        if (!"student".equals(SecurityUtils.getCurrentUserRole())) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
        Long classId = SecurityUtils.getCurrentUserClassId();
        if (classId == null) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
        Long count = courseClassMapper.selectCount(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getCourseId, course.getId())
                .eq(CourseClass::getClassId, classId));
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.COURSE_ACCESS_DENIED);
        }
    }

    public String paperContentForCurrentUser(ExamPaper paper) {
        if (paper == null || paper.getContent() == null || !"student".equals(SecurityUtils.getCurrentUserRole())) {
            return paper != null ? paper.getContent() : null;
        }
        try {
            JsonNode root = JSON.readTree(paper.getContent());
            redactStudentFields(root);
            return JSON.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("Invalid exam paper schema hidden from student: paperId={}", paper.getId(), e);
            return "{}";
        }
    }

    private void redactStudentFields(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            STUDENT_HIDDEN_FIELDS.forEach(object::remove);
            object.elements().forEachRemaining(this::redactStudentFields);
        } else if (node.isArray()) {
            node.elements().forEachRemaining(this::redactStudentFields);
        }
    }

    private List<ExamSubmissionVO> buildSubmissionInbox(Long examId, CourseRoster roster, List<ExamSubmission> submissions) {
        Map<Long, ExamSubmission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(ExamSubmission::getStudentId, sub -> sub, (left, right) -> left));
        return roster.students().stream()
                .map(student -> {
                    ExamSubmission submission = submissionMap.get(student.getId());
                    return ExamSubmissionVO.builder()
                            .id(submission != null ? submission.getId() : null)
                            .submissionId(submission != null ? submission.getId() : null)
                            .examId(examId)
                            .studentId(student.getId())
                            .studentName(student.getName())
                            .studentNo(student.getStudentNo())
                            .classId(student.getClassId())
                            .className(roster.displayClassName(student.getClassId()))
                            .answers(submission != null ? submission.getAnswers() : null)
                            .score(submission != null ? submission.getScore() : null)
                            .status(submission != null ? submission.getStatus() : "not_submitted")
                            .canResubmit(submission != null && SubmissionStatus.canResubmit(submission.getStatus()))
                            .returnReason(submission != null ? submission.getReturnReason() : null)
                            .returnedAt(submission != null ? submission.getReturnedAt() : null)
                            .startedAt(submission != null ? submission.getStartedAt() : null)
                            .revisionCount(submission != null ? Optional.ofNullable(submission.getRevisionCount()).orElse(0) : 0)
                            .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                            .createdAt(submission != null ? submission.getCreatedAt() : null)
                            .build();
                })
                .toList();
    }
}
