package com.seniorapp.controller;

import com.seniorapp.dto.StudentIdCheckRequest;
import com.seniorapp.dto.StudentIdValidityResponse;
import com.seniorapp.service.ValidStudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {

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
        return ResponseEntity.ok(body);
    }
}
