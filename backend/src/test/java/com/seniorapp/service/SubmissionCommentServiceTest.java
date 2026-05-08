package com.seniorapp.service;

import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.SubmissionComment;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.SubmissionCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionCommentServiceTest {

    @Mock
    private SubmissionCommentRepository commentRepository;
    @Mock
    private DeliverableSubmissionRepository submissionRepository;

    @InjectMocks
    private SubmissionCommentService commentService;

    private DeliverableSubmission submission;

    @BeforeEach
    void setUp() {
        submission = new DeliverableSubmission();
        submission.setId(1L);
    }

    @Test
    void addComment_Success() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        SubmissionComment result = commentService.addComment(1L, 100L, "Looks good!");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Looks good!");
        assertThat(result.getAuthorUserId()).isEqualTo(100L);
        verify(commentRepository).save(any());
    }

    @Test
    void addComment_NotFound() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> commentService.addComment(1L, 100L, "test"));
    }
}
