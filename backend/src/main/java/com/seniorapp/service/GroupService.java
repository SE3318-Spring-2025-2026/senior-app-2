package com.seniorapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {
    public void createGroup(String studentId, String groupName, String projectId) {
        if (studentId == null || groupName == null || projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields are missing.");
        }
        // Basit simülasyon (İleride DB eklenecek)
        System.out.println("Grup kuruluyor: " + groupName);
    }
}