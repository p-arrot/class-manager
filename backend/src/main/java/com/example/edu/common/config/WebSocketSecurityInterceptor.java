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
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Authenticate STOMP frames and scope task subscriptions to the teacher's course. */
@Component
@RequiredArgsConstructor
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Pattern TASK_DESTINATION = Pattern.compile("^/topic/task/(\\d+)(?:/count)?$");

    private final JwtUtils jwtUtils;
    private final TaskMapper taskMapper;
    private final LessonMapper lessonMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            LoginUser loginUser = authenticate(accessor.getFirstNativeHeader("Authorization"));
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities()));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private LoginUser authenticate(String header) {
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("WebSocket authentication required");
        }
        String token = header.substring(7);
        if (!jwtUtils.validateToken(token)) throw new IllegalArgumentException("Invalid WebSocket token");
        return jwtUtils.parseToken(token);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)
                || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw new IllegalArgumentException("WebSocket authentication required");
        }
        Matcher matcher = TASK_DESTINATION.matcher(accessor.getDestination() == null ? "" : accessor.getDestination());
        if (!matcher.matches()) throw new IllegalArgumentException("Unsupported subscription destination");
        if ("admin".equals(user.getRole())) return;
        if (!"teacher".equals(user.getRole())) {
            throw new IllegalArgumentException("Only teachers can subscribe to task updates");
        }

        Long taskId = Long.valueOf(matcher.group(1));
        Task task = taskMapper.selectById(taskId);
        Lesson lesson = task == null ? null : lessonMapper.selectById(task.getLessonId());
        Semester semester = lesson == null ? null : semesterMapper.selectById(lesson.getSemesterId());
        Course course = semester == null ? null : courseMapper.selectById(semester.getCourseId());
        if (course == null || !user.getUserId().equals(course.getTeacherId())) {
            throw new IllegalArgumentException("Task subscription denied");
        }
    }
}
