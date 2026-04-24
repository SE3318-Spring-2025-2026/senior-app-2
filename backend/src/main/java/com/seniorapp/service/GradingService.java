package com.seniorapp.service;

import com.seniorapp.dto.ScoreOverrideRequest;
import com.seniorapp.entity.AuditLog;
import com.seniorapp.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GradingService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public AuditLog overrideScore(Long auditId, ScoreOverrideRequest request) { // UUID yerine Long oldu
        // 1. Find the Audit record (Throw 404 if not found)
        AuditLog auditLog = auditLogRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found"));

        // 2. Validation: Does the studentId match this auditId? 
        // getUser().getId() yerine doğrudan getUserId() kullanıyoruz.
        if (!auditLog.getUserId().equals(request.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID does not match the provided Audit record");
        }

        // 3. Update the score and save it to D6 (Database)
        auditLog.setScore(request.getScore());
        return auditLogRepository.save(auditLog);
    }
}