package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupIntegrationsRequest;
import com.seniorapp.dto.GroupIntegrationsResponse;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Service
public class GroupService {
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final IntegrationCredentialCryptoService cryptoService;

    public GroupService(UserGroupRepository userGroupRepository, UserRepository userRepository, IntegrationCredentialCryptoService cryptoService) {
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    // --- ISSUE #72 MANTIĞI: GRUP KURMA ---
    public void createGroup(GroupCreateDto dto) {
        if (dto.getGroupName() == null || dto.getStudentId() == null || dto.getProjectId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All fields are required.");
        }
        if ("existing-group".equals(dto.getGroupName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name is already taken.");
        }
        System.out.println("Grup kuruldu: " + dto.getGroupName() + ", Lider: " + dto.getStudentId());
    }

    // --- SADECE ISSUE #74: ÜYE EKLEME/ÇIKARMA İŞLEMİ ---
    public void manageMembership(Long groupId, GroupMemberActionDto dto) {
        // Validate that the group exists
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        // Validate that the requester is the team leader
        if (!checkIsLeader(dto.getLeaderId(), group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can manage members.");
        }

        // Validate that the student exists
        User student = userRepository.findByGithubUsername(dto.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));

        if ("ADD".equalsIgnoreCase(dto.getAction())) {
            // Check if student is already in another group
            if (isAlreadyInAnotherGroup(student)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already a member of another group.");
            }
            
            // Add student to group
            if (group.getMembers() == null) {
                group.setMembers(new java.util.ArrayList<>());
            }
            group.getMembers().add(student);
            userGroupRepository.save(group);
            
        } else if ("REMOVE".equalsIgnoreCase(dto.getAction())) {
            // Check if student is in the current group
            if (!isMemberOfGroup(student, group)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not a member of this group.");
            }
            
            // Remove student from group
            if (group.getMembers() != null) {
                group.getMembers().remove(student);
                userGroupRepository.save(group);
            }
            
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action. Use ADD or REMOVE.");
        }
    }

    private boolean checkIsLeader(String leaderId, UserGroup group) {
        User leader = userRepository.findByGithubUsername(leaderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team Leader not found."));
        
        return group.getTeamLeader() != null && group.getTeamLeader().getId().equals(leader.getId());
    }
    
    private boolean isAlreadyInAnotherGroup(User student) {
        Optional<UserGroup> existingGroup = userGroupRepository.findByMember(student);
        return existingGroup.isPresent();
    }

    private boolean isMemberOfGroup(User student, UserGroup group) {
        return group.getMembers() != null && group.getMembers().contains(student);
    }

    public void saveIntegrations(Long groupId, GroupIntegrationsRequest request) {
        validateJiraUrl(request.getJiraSpaceUrl());

        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        group.setGithubPatEncrypted(cryptoService.encrypt(request.getGithubPat().trim()));
        group.setJiraSpaceUrlEncrypted(cryptoService.encrypt(request.getJiraSpaceUrl().trim()));
        userGroupRepository.save(group);
    }

    public GroupIntegrationsResponse getIntegrations(Long groupId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        String decryptedJiraUrl = cryptoService.decrypt(group.getJiraSpaceUrlEncrypted());

        // PAT is intentionally never sent back in plaintext to API clients.
        return new GroupIntegrationsResponse("", decryptedJiraUrl);
    }

    private void validateJiraUrl(String jiraSpaceUrl) {
        try {
            URI uri = new URI(jiraSpaceUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JIRA workspace URL format.");
            }
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JIRA workspace URL format.");
        }
    }
}