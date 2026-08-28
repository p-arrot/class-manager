package com.example.edu.common.config;

import com.example.edu.common.security.JwtUtils;
import com.example.edu.common.security.LoginUser;
import com.example.edu.modules.course.entity.Course;
import com.example.edu.modules.course.entity.Lesson;
import com.example.edu.modules.course.entity.Semester;
import com.example.edu.modules.course.mapper.CourseMapper;
import com.example.edu.modules.course.mapper.LessonMapper;
import com.example.edu.modules.course.mapper.SemesterMapper;
import com.example.edu.modules.task.entity.Task;
import com.example.edu.modules.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityInterceptorTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private TaskMapper taskMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private SemesterMapper semesterMapper;
    @Mock private CourseMapper courseMapper;

    @Test
    void connectWithoutBearerTokenIsRejected() {
        WebSocketSecurityInterceptor interceptor = newInterceptor();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authentication");
    }

    @Test
    void teacherCannotSubscribeToAnotherTeachersTask() {
        WebSocketSecurityInterceptor interceptor = newInterceptor();
        Task task = new Task();
        task.setId(9L);
        task.setLessonId(19L);
        Lesson lesson = new Lesson();
        lesson.setId(19L);
        lesson.setSemesterId(29L);
        Semester semester = new Semester();
        semester.setId(29L);
        semester.setCourseId(39L);
        Course course = new Course();
        course.setId(39L);
        course.setTeacherId(88L);
        when(taskMapper.selectById(9L)).thenReturn(task);
        when(lessonMapper.selectById(19L)).thenReturn(lesson);
        when(semesterMapper.selectById(29L)).thenReturn(semester);
        when(courseMapper.selectById(39L)).thenReturn(course);

        LoginUser teacher = new LoginUser(77L, "teacher", "teacher", null);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/task/9");
        accessor.setUser(new UsernamePasswordAuthenticationToken(teacher, null, teacher.getAuthorities()));

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("denied");
    }

    @Test
    void validConnectSetsAuthenticatedPrincipal() {
        WebSocketSecurityInterceptor interceptor = newInterceptor();
        LoginUser user = new LoginUser(77L, "teacher", "teacher", null);
        when(jwtUtils.validateToken(anyString())).thenReturn(true);
        when(jwtUtils.parseToken("token")).thenReturn(user);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer token");

        Message<?> result = interceptor.preSend(message(accessor), mock(MessageChannel.class));

        assertThat(StompHeaderAccessor.wrap(result).getUser()).isNotNull();
    }

    private WebSocketSecurityInterceptor newInterceptor() {
        return new WebSocketSecurityInterceptor(jwtUtils, taskMapper, lessonMapper, semesterMapper, courseMapper);
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
