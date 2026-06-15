package com.example.edu.common.config;

import com.example.edu.common.security.JwtUtils;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.task.controller.TaskController;
import com.example.edu.modules.task.service.TaskService;
import com.example.edu.modules.task.vo.TaskResultVO;
import com.example.edu.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@ContextConfiguration(classes = {SecurityConfig.class, JwtAuthenticationFilter.class, TaskController.class})
class TaskSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserMapper userMapper;

    @Test
    void studentCanViewOwnTaskResultBeforeBroadTaskRule() throws Exception {
        when(taskService.getMyResult(eq(54L), eq(101L))).thenReturn(TaskResultVO.builder().status("graded").build());

        mockMvc.perform(get("/api/tasks/54/my-result")
                        .with(authentication(auth(101L, "student"))))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotUseStudentTaskResultEndpoint() throws Exception {
        mockMvc.perform(get("/api/tasks/54/my-result")
                        .with(authentication(auth(9L, "teacher"))))
                .andExpect(status().isForbidden());
    }

    private static UsernamePasswordAuthenticationToken auth(Long userId, String role) {
        LoginUser user = new LoginUser(userId, role + userId, role, null);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
