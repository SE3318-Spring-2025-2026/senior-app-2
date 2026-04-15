package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupMemberActionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

    public void createGroup(GroupCreateDto dto) {
        if (dto.getGroupName() == null || dto.getStudentId() == null || dto.getProjectId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields.");
        }
        System.out.println("Grup kuruldu: " + dto.getGroupName());
    }

    public void manageMembership(Long groupId, GroupMemberActionDto dto) {
        if (!"leader-123".equals(dto.getLeaderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Leader can manage members.");
        }
        System.out.println("Üye işlemi yapıldı.");
    }
}