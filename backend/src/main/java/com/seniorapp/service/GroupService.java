package com.seniorapp.service;

import com.seniorapp.dto.GroupMemberActionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

    // --- ISSUE #72: GRUP KURMA İŞLEMİ ---
    public void createGroup(String studentId, String groupName, String projectId) {
        if (studentId == null || groupName == null || projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields are missing.");
        }
        System.out.println("Grup kuruluyor: " + groupName);
    }

    // --- ISSUE #74: ÜYE EKLEME/ÇIKARMA İŞLEMİ ---
    public void manageMembership(Long groupId, GroupMemberActionDto dto) {
        // 1. Yetki Kontrolü
        boolean isLeader = checkIsLeader(dto.getLeaderId(), groupId); 
        if (!isLeader) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can manage members.");
        }

        // 2. Öğrenci var mı kontrolü
        if (dto.getStudentId() == null || dto.getStudentId().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found.");
        }

        // 3. Action Mantığı (Ekle/Çıkar)
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

    // --- YARDIMCI METODLAR (Dummy Data) ---
    private boolean checkIsLeader(String leaderId, Long groupId) { 
        return "leader-123".equals(leaderId); 
    }
    
    private boolean isAlreadyInAnotherGroup(String studentId) { 
        return false; 
    }
}