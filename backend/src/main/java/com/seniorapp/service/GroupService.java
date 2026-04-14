package com.seniorapp.service;

import com.seniorapp.dto.GroupMemberActionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

    // --- SADECE ISSUE #74: ÜYE EKLEME/ÇIKARMA İŞLEMİ ---
    public void manageMembership(Long groupId, GroupMemberActionDto dto) {
        boolean isLeader = checkIsLeader(dto.getLeaderId(), groupId); 
        if (!isLeader) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can manage members.");
        }

        if (dto.getStudentId() == null || dto.getStudentId().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found.");
        }

        if ("ADD".equalsIgnoreCase(dto.getAction())) {
            if (isAlreadyInAnotherGroup(dto.getStudentId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already a member of another group.");
            }
            System.out.println(dto.getStudentId() + " gruba eklendi.");
        } 
        else if ("REMOVE".equalsIgnoreCase(dto.getAction())) {
            System.out.println(dto.getStudentId() + " gruptan çıkarıldı.");
        } 
        else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action. Use ADD or REMOVE.");
        }
    }

    private boolean checkIsLeader(String leaderId, Long groupId) { 
        return "leader-123".equals(leaderId); 
    }
    
    private boolean isAlreadyInAnotherGroup(String studentId) { 
        return false; 
    }
}