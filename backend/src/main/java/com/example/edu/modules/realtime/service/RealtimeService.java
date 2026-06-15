package com.example.edu.modules.realtime.service;

import com.example.edu.modules.task.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushSubmissionUpdate(Long taskId, SubmissionVO submission) {
        messagingTemplate.convertAndSend("/topic/task/" + taskId, submission);
        log.debug("WebSocket pushed: taskId={}, studentId={}", taskId, submission.getStudentId());
    }

    public void pushSubmissionCount(Long taskId, int count) {
        messagingTemplate.convertAndSend("/topic/task/" + taskId + "/count", count);
    }
}
