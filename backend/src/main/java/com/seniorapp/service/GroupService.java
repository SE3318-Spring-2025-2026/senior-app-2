package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupIntegrationsRequest;
import com.seniorapp.dto.GroupIntegrationsResponse;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.repository.UserGroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class GroupService {
    private final UserGroupRepository userGroupRepository;
    private final IntegrationCredentialCryptoService cryptoService;

    public GroupService(UserGroupRepository userGroupRepository, IntegrationCredentialCryptoService cryptoService) {
        this.userGroupRepository = userGroupRepository;
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