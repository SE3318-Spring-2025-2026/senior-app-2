package com.seniorapp.controller;

import com.seniorapp.entity.SubmissionComment;
import com.seniorapp.service.SubmissionCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/submissions/{submissionId}/comments")
public class SubmissionCommentController {

    private final SubmissionCommentService commentService;

    public SubmissionCommentController(SubmissionCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<SubmissionComment> addComment(@PathVariable Long submissionId,
                                                       @RequestAttribute("userId") Long userId,
                                                       @RequestBody CommentRequest req) {
        return ResponseEntity.ok(commentService.addComment(submissionId, userId, req.content()));
    }

    @GetMapping
    public ResponseEntity<List<SubmissionComment>> getComments(@PathVariable Long submissionId) {
        return ResponseEntity.ok(commentService.getComments(submissionId));
    }

    public record CommentRequest(String content) {}
}
