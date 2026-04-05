package com.seniorapp.controller;

import com.seniorapp.dto.StudentIdCheckRequest;
import com.seniorapp.dto.StudentIdValidityResponse;
import com.seniorapp.service.ValidStudentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private final ValidStudentService validStudentService;

    public StudentController(ValidStudentService validStudentService) {
        this.validStudentService = validStudentService;
    }

    /**
     * Step 1 of student GitHub login: verify whitelist entry and whether an account is linked.
     */
    @PostMapping("/student-ids/check-id-validity")
    public ResponseEntity<StudentIdValidityResponse> checkIdValidity(
            @Valid @RequestBody StudentIdCheckRequest request) {
        StudentIdValidityResponse body = validStudentService.checkStudentIdValidity(request.getStudentId());
        log.debug("Student ID check: valid={} linked={}", body.valid(), body.linked());
        return ResponseEntity.ok(body);
    }
}
