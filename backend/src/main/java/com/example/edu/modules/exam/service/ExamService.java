package com.example.edu.modules.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.course.service.CoursePermissionHelper;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.evaluation.service.QuestionScoreHelper;
import com.example.edu.modules.exam.entity.*;
import com.example.edu.modules.exam.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
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
        return paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getTeacherId, SecurityUtils.getCurrentUserId())
                .orderByDesc(ExamPaper::getCreatedAt));
    }

    public List<ExamPaper> listPaperByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return paperMapper.selectBatchIds(ids);
    }

    // Exams
    @Transactional
    public Exam createExam(Exam exam) {
        validateExam(exam);
        examMapper.insert(exam);
        auditLogService.record("创建考试", "exam", exam.getId(), exam.getName());
        return exam;
    }

    public List<Exam> listExams(Long semesterId) {
        return examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getSemesterId, semesterId)
                .orderByDesc(Exam::getCreatedAt));
    }

    @Transactional(rollbackFor = Exception.class)
    public Exam updateExam(Long id, Exam exam) {
        Exam existing = examMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        validateExam(exam);
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
        if (exam.getPaperId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择试卷");
        }
        if (paperMapper.selectById(exam.getPaperId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
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
        if (LocalDateTime.now().isAfter(exam.getEndTime()))
            throw new BizException(ErrorCode.TASK_DEADLINE_PASSED);

        ExamSubmission sub = submissionMapper.selectOne(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId)
                .eq(ExamSubmission::getStudentId, studentId));
        if (sub == null) {
            sub = new ExamSubmission();
            sub.setExamId(examId);
            sub.setStudentId(studentId);
        }
        sub.setAnswers(answers);
        sub.setStatus("submitted");
        sub.setSubmittedAt(LocalDateTime.now());
        if (sub.getId() == null) submissionMapper.insert(sub);
        else submissionMapper.updateById(sub);
        autoGradeExam(exam, sub);
        return sub;
    }

    public List<ExamSubmission> listSubmissions(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkTeacherOwnsExam(exam);
        return submissionMapper.selectList(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void gradeSubmission(Long submissionId, Integer score, boolean absent, List<DimensionScoreService.ScoreInput> dimensionScores) {
        ExamSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        Exam exam = examMapper.selectById(sub.getExamId());
        if (exam == null) throw new BizException(ErrorCode.NOT_FOUND);
        checkTeacherOwnsExam(exam);

        sub.setScore(absent ? 0 : score);
        sub.setStatus(absent ? "absent" : "graded");
        submissionMapper.updateById(sub);
        if (absent) {
            dimensionScoreService.replaceScores("exam", submissionId, sub.getStudentId(), List.of());
        } else if (dimensionScores != null && !dimensionScores.isEmpty()) {
            dimensionScoreService.replaceScores("exam", submissionId, sub.getStudentId(), dimensionScores);
        }
        auditLogService.record("考试评分", "exam_submission", submissionId, String.valueOf(score));
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

    private void checkTeacherOwnsExam(Exam exam) {
        Semester semester = semesterMapper.selectById(exam.getSemesterId());
        if (semester == null) throw new BizException(ErrorCode.SEMESTER_NOT_FOUND);
        Course course = courseMapper.selectById(semester.getCourseId());
        if (course == null) throw new BizException(ErrorCode.COURSE_NOT_FOUND);
        CoursePermissionHelper.checkTeacherOwnsCourse(course);
    }
}
