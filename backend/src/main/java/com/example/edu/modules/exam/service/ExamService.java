package com.example.edu.modules.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.security.SecurityUtils;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.exam.entity.*;
import com.example.edu.modules.exam.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final AuditLogService auditLogService;

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

    // Exams
    @Transactional
    public Exam createExam(Exam exam) {
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
        return sub;
    }

    public List<ExamSubmission> listSubmissions(Long examId) {
        return submissionMapper.selectList(new LambdaQueryWrapper<ExamSubmission>()
                .eq(ExamSubmission::getExamId, examId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void gradeSubmission(Long submissionId, Integer score, boolean absent) {
        ExamSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.SUBMISSION_NOT_FOUND);
        sub.setScore(absent ? 0 : score);
        sub.setStatus(absent ? "absent" : "graded");
        submissionMapper.updateById(sub);
        auditLogService.record("考试评分", "exam_submission", submissionId, String.valueOf(score));
    }
}
