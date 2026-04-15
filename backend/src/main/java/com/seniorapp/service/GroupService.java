package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupMemberActionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

    // --- ISSUE #72 LOGIC ---
    public void createGroup(GroupCreateDto dto) {
        // Acceptance Criteria: Validation
        if (dto.getGroupName() == null || dto.getStudentId() == null || dto.getProjectId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "groupName, studentId, and projectId are required.");
        }

        // Acceptance Criteria: Conflict Check (Mock logic)
        if ("taken-name".equals(dto.getGroupName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name is taken.");
        }

        System.out.println("Grup kuruldu: " + dto.getGroupName() + " (Lider: " + dto.getStudentId() + ")");
    }

    // --- ISSUE #74 LOGIC ---
    public void manageMembership(Long groupId, GroupMemberActionDto dto) {
        if (!"leader-123".equals(dto.getLeaderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can manage members.");
        }
        System.out.println("Üye işlemi (" + dto.getAction() + "): " + dto.getStudentId());
    }
}