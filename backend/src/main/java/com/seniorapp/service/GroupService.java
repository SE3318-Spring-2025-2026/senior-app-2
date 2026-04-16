package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.User;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

@Service
public class GroupService {

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserRepository userRepository;

    // --- ISSUE #71 & #72 FIX: GERÇEK GRUP KURMA ---
    public void createGroup(GroupCreateDto dto, Long currentUserId) {
        if (dto.getGroupName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name is required.");
        }

        // 1. Conflict Kontrolü (Aynı isimde grup var mı?)
        if (userGroupRepository.existsByGroupName(dto.getGroupName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name is already taken.");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        // 2. Kullanıcı zaten bir grupta mı?
        if (userGroupRepository.existsByMembersIdOrTeamLeaderId(currentUserId, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already in a group.");
        }

        // 3. Grubu kur ve Lider ata
        UserGroup group = new UserGroup();
        group.setGroupName(dto.getGroupName());
        group.setTeamLeader(currentUser);
        
        if (group.getMembers() == null) group.setMembers(new ArrayList<>());
        group.getMembers().add(currentUser);

        userGroupRepository.save(group);
    }

    // --- ISSUE #73 & #74 FIX: ÜYE YÖNETİMİ ---
    public void manageMembership(Long groupId, GroupMemberActionDto dto, Long currentUserId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        // Yetki Kontrolü: İsteği atan kişi takım lideri mi? (BUG #2 FIX: 403 döner)
        if (group.getTeamLeader() == null || !group.getTeamLeader().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can manage members.");
        }

        User targetUser = userRepository.findById(Long.valueOf(dto.getStudentId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target student not found."));

        if ("ADD".equalsIgnoreCase(dto.getAction())) {
            if (userGroupRepository.existsByMembersIdOrTeamLeaderId(targetUser.getId(), targetUser.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already in a group.");
            }
            group.getMembers().add(targetUser);
            userGroupRepository.save(group);
            
        } else if ("REMOVE".equalsIgnoreCase(dto.getAction())) {
            boolean removed = group.getMembers().removeIf(u -> u.getId().equals(targetUser.getId()));
            if (!removed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not in this group.");
            }
            userGroupRepository.save(group);
        }
    }
}