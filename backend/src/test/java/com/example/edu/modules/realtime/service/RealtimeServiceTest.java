package com.example.edu.modules.realtime.service;

import com.example.edu.modules.task.vo.SubmissionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RealtimeServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    @Test
    void submissionUpdateDoesNotBroadcastSubmissionContent() {
        RealtimeService service = new RealtimeService(messagingTemplate);
        SubmissionVO submission = SubmissionVO.builder()
                .id(11L).taskId(9L).studentId(101L).status("submitted")
                .content("student's private answer")
                .submittedAt(LocalDateTime.of(2026, 7, 10, 9, 0))
                .build();
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);

        service.pushSubmissionUpdate(9L, submission);

        verify(messagingTemplate).convertAndSend(eq("/topic/task/9"), event.capture());
        assertThat(event.getValue()).isInstanceOf(RealtimeService.SubmissionUpdate.class);
        RealtimeService.SubmissionUpdate update = (RealtimeService.SubmissionUpdate) event.getValue();
        assertThat(update.submissionId()).isEqualTo(11L);
        assertThat(update.studentId()).isEqualTo(101L);
        assertThat(update.status()).isEqualTo("submitted");
        assertThat(event.getValue().toString()).doesNotContain("private answer");
    }
}
