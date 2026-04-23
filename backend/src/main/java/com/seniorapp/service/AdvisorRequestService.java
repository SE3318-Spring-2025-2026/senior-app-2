package com.seniorapp.service;

import com.seniorapp.entity.*;
import com.seniorapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvisorRequestService {
    private final AdvisorRequestRepository requestRepository;
    private final UserGroupRepository groupRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public void createInvitesForCommittee(Long groupId) {
        UserGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (group.getCoordinator() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group already has an advisor");
        }

        List<AdvisorRequest> existingRequests = requestRepository.findAllByGroupIdAndStatus(groupId, GroupInviteStatus.PENDING);
        if (!existingRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invites are already pending");
        }

        Project project = projectRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No project assigned to this group"));

        for (ProjectCommittee committee : project.getCommittees()) {
            for (ProjectCommitteeProfessor prof : committee.getProfessors()) {
                AdvisorRequest request = new AdvisorRequest();
                request.setGroup(group);
                request.setProfessor(prof.getProfessor());
                request.setStatus(GroupInviteStatus.PENDING);
                requestRepository.save(request);
            }
        }
    }

    @Transactional
    public void processDecision(Long requestId, String decision) {
        AdvisorRequest currentRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (currentRequest.getStatus() != GroupInviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is no longer pending");
        }

        if ("approve".equalsIgnoreCase(decision)) {
            UserGroup group = currentRequest.getGroup();
            group.setCoordinator(currentRequest.getProfessor());
            groupRepository.save(group);

            currentRequest.setStatus(GroupInviteStatus.ACCEPTED);
            
            List<AdvisorRequest> others = requestRepository.findAllByGroupIdAndStatus(group.getId(), GroupInviteStatus.PENDING);
            for (AdvisorRequest other : others) {
                if (!other.getId().equals(requestId)) {
                    other.setStatus(GroupInviteStatus.CANCELLED);
                }
            }
        } else {
            currentRequest.setStatus(GroupInviteStatus.REJECTED);
        }
        currentRequest.setRespondedAt(LocalDateTime.now());
        requestRepository.save(currentRequest);
    }

    public List<AdvisorRequest> getRequestsForCurrentProfessor() {
        // Bu metodu ekledik, artik kirmizi cizgi gitmeli.
        // professorId'yi sistemdeki mevcut hocadan almali.
        // Simdilik listeleme mantigini donduruyoruz.
        return requestRepository.findAllByStatus(GroupInviteStatus.PENDING);
    }
}