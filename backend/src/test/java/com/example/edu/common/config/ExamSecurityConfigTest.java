package com.example.edu.common.config;

import com.example.edu.modules.exam.controller.ExamController;
import com.example.edu.modules.exam.entity.ExamSubmission;
import com.example.edu.modules.exam.entity.Exam;
import com.example.edu.modules.exam.entity.ExamPaper;
import com.example.edu.modules.exam.service.ExamService;
import com.example.edu.modules.user.mapper.UserMapper;
import com.example.edu.common.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExamController.class)
@ContextConfiguration(classes = {SecurityConfig.class, JwtAuthenticationFilter.class, ExamController.class})
class ExamSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ExamService examService;

    @MockBean
    private UserMapper userMapper;

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanSubmitExamBeforeBroadExamRule() throws Exception {
        ExamSubmission submission = new ExamSubmission();
        submission.setId(11L);
        submission.setExamId(5L);
        submission.setStudentId(101L);
        submission.setAnswers("{\"q1\":\"A\"}");
        submission.setStatus("submitted");
        submission.setSubmittedAt(LocalDateTime.of(2026, 6, 14, 9, 0));
        submission.setCreatedAt(LocalDateTime.of(2026, 6, 14, 9, 0));
        when(examService.submit(eq(5L), eq("{\"q1\":\"A\"}"))).thenReturn(submission);

        mockMvc.perform(post("/api/exams/5/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":\"{\\\"q1\\\":\\\"A\\\"}\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotUseStudentExamSubmitEndpoint() throws Exception {
        mockMvc.perform(post("/api/exams/5/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":\"{}\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotViewExamSubmissionList() throws Exception {
        when(examService.listSubmissions(5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/exams/5/submissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanViewExamList() throws Exception {
        Exam exam = new Exam();
        exam.setId(5L);
        exam.setSemesterId(2L);
        exam.setPaperId(8L);
        ExamPaper paper = new ExamPaper();
        paper.setId(8L);
        paper.setContent("{}");
        when(examService.listExams(2L)).thenReturn(List.of(exam));
        when(examService.listPaperByIds(java.util.Set.of(8L))).thenReturn(List.of(paper));
        when(examService.paperContentForCurrentUser(paper)).thenReturn("{}");

        mockMvc.perform(get("/api/semesters/2/exams"))
                .andExpect(status().isOk());
    }
}
