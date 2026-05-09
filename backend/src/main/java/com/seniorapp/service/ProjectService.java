package com.seniorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.project.ProjectDtos.*;
import com.seniorapp.dto.project.ProjectGradingSummaryDto;
import com.seniorapp.entity.*;
import com.seniorapp.repository.ProjectCommitteeProfessorRepository;
import com.seniorapp.repository.ProjectCommitteeRepository;
import com.seniorapp.repository.ProjectDeliverableRubricRepository;
import com.seniorapp.repository.ProjectEvaluationRubricRepository;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectIssueSyncSnapshotRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectTemplateRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.service.grading.PdfGradingEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectGroupAssignmentRepository assignmentRepository;
    private final ProjectCommitteeRepository projectCommitteeRepository;
    private final ProjectCommitteeProfessorRepository projectCommitteeProfessorRepository;
    private final ProjectDeliverableRubricRepository projectDeliverableRubricRepository;
    private final ProjectEvaluationRubricRepository projectEvaluationRubricRepository;
    private final UserRepository userRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final SecureOutboundApiService secureOutboundApiService;
    private final JiraProvisioningService jiraProvisioningService;
    private final ProjectGithubIssueSyncService projectGithubIssueSyncService;
    private final JiraGithubDailySyncService jiraGithubDailySyncService;
    private final ProjectIssueSyncSnapshotRepository projectIssueSyncSnapshotRepository;
    private final ObjectMapper objectMapper;
    private final PdfGradingEngineService pdfGradingEngineService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectGroupAssignmentRepository assignmentRepository,
            ProjectCommitteeRepository projectCommitteeRepository,
            ProjectCommitteeProfessorRepository projectCommitteeProfessorRepository,
            ProjectDeliverableRubricRepository projectDeliverableRubricRepository,
            ProjectEvaluationRubricRepository projectEvaluationRubricRepository,
            UserRepository userRepository,
            UserGroupMemberRepository userGroupMemberRepository,
            SecureOutboundApiService secureOutboundApiService,
            JiraProvisioningService jiraProvisioningService,
            ProjectGithubIssueSyncService projectGithubIssueSyncService,
            JiraGithubDailySyncService jiraGithubDailySyncService,
            ProjectIssueSyncSnapshotRepository projectIssueSyncSnapshotRepository,
            ObjectMapper objectMapper,
            PdfGradingEngineService pdfGradingEngineService
    ) {
        this.projectRepository = projectRepository;
        this.projectTemplateRepository = projectTemplateRepository;
        this.assignmentRepository = assignmentRepository;
        this.projectCommitteeRepository = projectCommitteeRepository;
        this.projectCommitteeProfessorRepository = projectCommitteeProfessorRepository;
        this.projectDeliverableRubricRepository = projectDeliverableRubricRepository;
        this.projectEvaluationRubricRepository = projectEvaluationRubricRepository;
        this.userRepository = userRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.secureOutboundApiService = secureOutboundApiService;
        this.jiraProvisioningService = jiraProvisioningService;
        this.projectGithubIssueSyncService = projectGithubIssueSyncService;
        this.jiraGithubDailySyncService = jiraGithubDailySyncService;
        this.projectIssueSyncSnapshotRepository = projectIssueSyncSnapshotRepository;
        this.objectMapper = objectMapper;
        this.pdfGradingEngineService = pdfGradingEngineService;
    }

    @Transactional
    public Long createProject(CreateProjectRequest request, Long createdByUserId) {
        if (createdByUserId == null) {
            throw new IllegalArgumentException("Authenticated user id is required.");
        }
        if (request.getTemplateId() == null) {
            throw new IllegalArgumentException("templateId is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required.");
        }
        if (request.getTerm() == null || request.getTerm().isBlank()) {
            throw new IllegalArgumentException("term is required.");
        }

        Long safeTemplateId = Objects.requireNonNull(request.getTemplateId(), "templateId is required.");
        ProjectTemplate template = projectTemplateRepository.findById(safeTemplateId)
                .orElseThrow(() -> new NoSuchElementException("Project template not found: " + request.getTemplateId()));

        JsonNode templateRoot;
        try {
            templateRoot = objectMapper.readTree(template.getTemplateJson());
        } catch (Exception e) {
            throw new IllegalStateException("Stored template JSON is invalid.", e);
        }

        JsonNode sprintsNode = templateRoot.get("sprints");
        if (sprintsNode == null || !sprintsNode.isArray() || sprintsNode.isEmpty()) {
            throw new IllegalArgumentException("Template does not include valid sprints.");
        }

        Project project = new Project();
        project.setTemplate(template);
        project.setTitle(request.getTitle().trim());
        project.setTerm(request.getTerm().trim());
        project.setStatus(ProjectStatus.DRAFT);
        project.setCreatedByUserId(createdByUserId);
        project.setSprints(buildProjectSprints(project, sprintsNode));

        Project saved = projectRepository.save(project);
        if (request.getGroupId() != null) {
            assignGroup(saved.getId(), request.getGroupId(), createdByUserId);
        }
        return Objects.requireNonNull(saved.getId());
    }

    @Transactional
    public AssignmentResponse assignGroup(Long projectId, Long groupId, Long assignedByUserId) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId is required.");
        }
        if (assignedByUserId == null) {
            throw new IllegalArgumentException("Authenticated user id is required.");
        }

        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        Project project = projectRepository.findById(safeProjectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));

        if (project.getGroupId() != null) {
            throw new IllegalArgumentException("Project already has a groupId.");
        }
        assignmentRepository.findByProjectIdAndActiveTrue(safeProjectId).ifPresent(existing -> {
            throw new IllegalArgumentException("Project already has an active group assignment.");
        });
        assignmentRepository.findByGroupIdAndActiveTrue(groupId).ifPresent(existing -> {
            throw new IllegalArgumentException("Group is already assigned to another active project.");
        });
        project.setGroupId(groupId);
        projectRepository.save(project);

        ProjectGroupAssignment assignment = new ProjectGroupAssignment();
        assignment.setProject(project);
        assignment.setGroupId(groupId);
        assignment.setAssignedByUserId(assignedByUserId);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setActive(true);
        ProjectGroupAssignment saved = assignmentRepository.save(assignment);
        createGithubRepoOnAssignmentRequired(project, groupId, assignedByUserId);
        createJiraWorkspaceOnAssignmentRequired(project, groupId);

        return new AssignmentResponse("success", safeProjectId, saved.getGroupId(), saved.getAssignedAt());
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> listProjects(String term, Long templateId, Long groupId, Long requesterUserId, Role requesterRole) {
        Set<Long> allowedStudentGroupIds = null;
        if (requesterRole == Role.STUDENT) {
            if (requesterUserId == null) {
                throw new IllegalArgumentException("Authenticated user id is required.");
            }
            allowedStudentGroupIds = new HashSet<>();
            for (UserGroupMember m : userGroupMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    requesterUserId, GroupInviteStatus.ACCEPTED)) {
                if (m.getGroup() != null && m.getGroup().getId() != null) {
                    allowedStudentGroupIds.add(m.getGroup().getId());
                }
            }
        }

        final Set<Long> studentGroups = allowedStudentGroupIds;
        return projectRepository.findAll().stream()
                .filter(project -> term == null || term.isBlank() || project.getTerm().equalsIgnoreCase(term.trim()))
                .filter(project -> {
                    if (templateId == null) return true;
                    ProjectTemplate template = project.getTemplate();
                    return template != null && Objects.equals(template.getId(), templateId);
                })
                .filter(project -> {
                    if (groupId == null) return true;
                    return Objects.equals(project.getGroupId(), groupId);
                })
                .filter(project -> {
                    if (studentGroups == null) return true;
                    Long projectGroupId = project.getGroupId();
                    return projectGroupId != null && studentGroups.contains(projectGroupId);
                })
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public ProjectDetail getProjectDetail(Long projectId, Long requesterUserId, Role requesterRole) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        jiraGithubDailySyncService.syncProjectNow(safeProjectId);
        Project project = projectRepository.findById(safeProjectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
        warmProjectSprintCollections(project);

        List<Long> deliverableIds = new ArrayList<>();
        List<Long> evaluationIds = new ArrayList<>();
        for (ProjectSprint sprint : project.getSprints()) {
            for (ProjectDeliverable d : sprint.getDeliverables()) {
                if (d.getId() != null) {
                    deliverableIds.add(d.getId());
                }
            }
            for (ProjectEvaluation ev : sprint.getEvaluations()) {
                if (ev.getId() != null) {
                    evaluationIds.add(ev.getId());
                }
            }
        }

        Map<Long, List<ProjectDeliverableRubric>> deliverableRubricsFromDb = null;
        if (!deliverableIds.isEmpty()) {
            deliverableRubricsFromDb =
                    projectDeliverableRubricRepository.findByDeliverable_IdIn(deliverableIds).stream()
                            .collect(Collectors.groupingBy(r -> r.getDeliverable().getId()));
        }

        Map<Long, List<ProjectEvaluationRubric>> evaluationRubricsFromDb = null;
        if (!evaluationIds.isEmpty()) {
            evaluationRubricsFromDb =
                    projectEvaluationRubricRepository.findByEvaluation_IdIn(evaluationIds).stream()
                            .collect(Collectors.groupingBy(r -> r.getEvaluation().getId()));
        }

        ProjectDetail detail = toDetail(project, deliverableRubricsFromDb, evaluationRubricsFromDb);
        assignmentRepository.findByProjectIdAndActiveTrue(safeProjectId).ifPresent(a -> {
            if (a.getCommittee() != null) {
                detail.setActiveCommitteeId(a.getCommittee().getId());
                detail.setActiveCommitteeName(a.getCommittee().getName());
            }
        });
        Long gid = project.getGroupId();
        if (gid != null && gid > 0) {
            detail.setGradingSummary(pdfGradingEngineService.buildSummary(project, gid));
        }
        return detail;
    }

    /** Force-load sprint deliverables, evaluations and rubrics (avoids empty collections with lazy loading). */
    private void warmProjectSprintCollections(Project project) {
        if (project.getTemplate() != null) {
            project.getTemplate().getId();
        }
        for (ProjectSprint sprint : project.getSprints()) {
            sprint.getDeliverables().size();
            sprint.getEvaluations().size();
            for (ProjectDeliverable d : sprint.getDeliverables()) {
                d.getRubrics().size();
            }
            for (ProjectEvaluation ev : sprint.getEvaluations()) {
                ev.getRubrics().size();
            }
        }
    }

    @Transactional
    public CommitteeDto createCommittee(Long projectId, String name) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        Project project = projectRepository.findById(safeProjectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));

        String resolvedName = name == null || name.isBlank()
                ? "Committee " + (projectCommitteeRepository.findByProjectIdOrderByIdAsc(safeProjectId).size() + 1)
                : name.trim();

        ProjectCommittee committee = new ProjectCommittee();
        committee.setProject(project);
        committee.setName(resolvedName);
        ProjectCommittee saved = projectCommitteeRepository.save(committee);
        return toCommitteeDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CommitteeDto> listCommittees(Long projectId) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        if (!projectRepository.existsById(safeProjectId)) {
            throw new NoSuchElementException("Project not found: " + projectId);
        }
        return projectCommitteeRepository.findByProjectIdOrderByIdAsc(safeProjectId).stream()
                .map(this::toCommitteeDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectPullRequestDto> listPullRequests(Long projectId, Long requesterUserId, Role requesterRole) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        // Reuse existing visibility rules.
        getProjectDetail(safeProjectId, requesterUserId, requesterRole);
        Project project = projectRepository.findById(safeProjectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
        if (project.getRepoFullName() == null || project.getRepoFullName().isBlank()) {
            return List.of();
        }
        String encryptedPat = resolveTemplateCreatorPat(project);
        if (encryptedPat == null || encryptedPat.isBlank()) {
            return List.of();
        }
        try {
            List<ProjectPullRequestDto> result = new ArrayList<>();
            int page = 1;
            while (true) {
                String endpoint = "https://api.github.com/repos/" + project.getRepoFullName()
                        + "/pulls?state=all&per_page=100&page=" + page;
                String body = secureOutboundApiService.executeGitHubApiCall(
                        encryptedPat, endpoint, null, HttpMethod.GET).getBody();
                JsonNode arr = objectMapper.readTree(body);
                if (!arr.isArray() || arr.isEmpty()) {
                    break;
                }
                for (JsonNode pr : arr) {
                    ProjectPullRequestDto dto = new ProjectPullRequestDto();
                    dto.setNumber(pr.path("number").isNumber() ? pr.path("number").asInt() : null);
                    dto.setTitle(pr.path("title").asText(null));
                    dto.setState(pr.path("state").asText(null));
                    dto.setMerged(!pr.path("merged_at").isNull());
                    dto.setHtmlUrl(pr.path("html_url").asText(null));
                    dto.setHeadRef(pr.path("head").path("ref").asText(null));
                    dto.setBaseRef(pr.path("base").path("ref").asText(null));
                    dto.setAuthor(pr.path("user").path("login").asText(null));
                    result.add(dto);
                }
                if (arr.size() < 100) {
                    break;
                }
                page++;
            }
            return result;
        } catch (Exception ex) {
            log.warn("Could not fetch PR list for project {}: {}", safeProjectId, ex.getMessage());
            return List.of();
        }
    }

    @Transactional
    public CommitteeDto addProfessorToCommittee(Long projectId, Long committeeId, Long professorUserId) {
        if (professorUserId == null) {
            throw new IllegalArgumentException("professorUserId is required.");
        }
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        Long safeCommitteeId = Objects.requireNonNull(committeeId, "committeeId is required.");
        ProjectCommittee committee = projectCommitteeRepository.findById(safeCommitteeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found: " + committeeId));
        if (!Objects.equals(committee.getProject().getId(), safeProjectId)) {
            throw new IllegalArgumentException("Committee does not belong to this project.");
        }

        User professor = userRepository.findById(professorUserId)
                .orElseThrow(() -> new NoSuchElementException("Professor not found: " + professorUserId));
        if (professor.getRole() != Role.PROFESSOR && professor.getRole() != Role.COORDINATOR) {
            throw new IllegalArgumentException("Selected user must be a professor or coordinator.");
        }

        boolean alreadyExists = committee.getProfessors().stream()
                .anyMatch(member -> Objects.equals(member.getProfessor().getId(), professorUserId));
        if (alreadyExists) {
            return toCommitteeDto(committee);
        }

        ProjectCommitteeProfessor member = new ProjectCommitteeProfessor();
        member.setCommittee(committee);
        member.setProfessor(professor);
        projectCommitteeProfessorRepository.save(member);

        ProjectCommittee reloaded = projectCommitteeRepository.findById(safeCommitteeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found after update: " + committeeId));
        return toCommitteeDto(reloaded);
    }

    @Transactional
    public void deleteCommittee(Long projectId, Long committeeId) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        Long safeCommitteeId = Objects.requireNonNull(committeeId, "committeeId is required.");
        ProjectCommittee committee = projectCommitteeRepository.findById(safeCommitteeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found: " + committeeId));
        if (!Objects.equals(committee.getProject().getId(), safeProjectId)) {
            throw new IllegalArgumentException("Committee does not belong to this project.");
        }
        projectCommitteeRepository.delete(committee);
    }

    @Transactional
    public CommitteeDto removeProfessorFromCommittee(Long projectId, Long committeeId, Long professorUserId) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        Long safeCommitteeId = Objects.requireNonNull(committeeId, "committeeId is required.");
        Long safeProfessorUserId = Objects.requireNonNull(professorUserId, "professorUserId is required.");

        ProjectCommittee committee = projectCommitteeRepository.findById(safeCommitteeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found: " + committeeId));
        if (!Objects.equals(committee.getProject().getId(), safeProjectId)) {
            throw new IllegalArgumentException("Committee does not belong to this project.");
        }

        committee.getProfessors().removeIf(member ->
                Objects.equals(member.getProfessor().getId(), safeProfessorUserId)
        );
        ProjectCommittee saved = projectCommitteeRepository.save(committee);
        return toCommitteeDto(saved);
    }

    @Transactional
    public CommitteeDto assignGroupCommittee(Long projectId, Long groupId, Long committeeId, Long requesterUserId, Role requesterRole) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        Long safeGroupId = Objects.requireNonNull(groupId, "groupId is required.");
        Long safeCommitteeId = Objects.requireNonNull(committeeId, "committeeId is required.");
        Long safeRequesterUserId = Objects.requireNonNull(requesterUserId, "requesterUserId is required.");
        Role safeRequesterRole = Objects.requireNonNull(requesterRole, "requesterRole is required.");

        ProjectGroupAssignment assignment = assignmentRepository.findByGroupIdAndActiveTrue(safeGroupId)
                .orElseThrow(() -> new NoSuchElementException("Active assignment not found for group: " + groupId));
        if (assignment.getProject() == null || !Objects.equals(assignment.getProject().getId(), safeProjectId)) {
            throw new IllegalArgumentException("Group is not assigned to this project.");
        }

        ProjectCommittee committee = projectCommitteeRepository.findById(safeCommitteeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found: " + committeeId));
        if (committee.getProject() == null || !Objects.equals(committee.getProject().getId(), safeProjectId)) {
            throw new IllegalArgumentException("Committee does not belong to this project.");
        }

        boolean allowed;
        if (safeRequesterRole == Role.ADMIN || safeRequesterRole == Role.COORDINATOR) {
            allowed = true;
        } else if (safeRequesterRole == Role.PROFESSOR) {
            allowed = projectCommitteeProfessorRepository
                    .findByCommitteeIdAndProfessor_Id(safeCommitteeId, safeRequesterUserId)
                    .isPresent();
        } else {
            allowed = false;
        }
        if (!allowed) {
            throw new IllegalArgumentException("You are not allowed to assign this committee.");
        }

        assignment.setCommittee(committee);
        assignmentRepository.save(assignment);
        return toCommitteeDto(committee);
    }

    @Transactional(readOnly = true)
    public List<ProfessorOptionDto> listProfessors() {
        List<ProfessorOptionDto> result = new ArrayList<>();
        result.addAll(userRepository.findByRole(Role.PROFESSOR).stream()
                .map(this::toProfessorOptionDto)
                .toList());
        result.addAll(userRepository.findByRole(Role.COORDINATOR).stream()
                .map(this::toProfessorOptionDto)
                .toList());
        return result;
    }

    private List<ProjectSprint> buildProjectSprints(Project project, JsonNode sprintsNode) {
        List<ProjectSprint> result = new ArrayList<>();
        for (JsonNode sprintNode : sprintsNode) {
            ProjectSprint sprint = new ProjectSprint();
            sprint.setProject(project);
            sprint.setSprintNo(sprintNode.path("sprintNo").asInt());
            sprint.setTitle(sprintNode.path("title").asText("Sprint " + sprint.getSprintNo()));
            sprint.setStartDate(parseDateOrNull(sprintNode.get("startDate")));
            sprint.setEndDate(parseDateOrNull(sprintNode.get("endDate")));

            sprint.setDeliverables(buildDeliverables(sprint, sprintNode.path("deliverables")));
            sprint.setEvaluations(buildEvaluations(sprint, sprintNode.path("evaluations")));
            result.add(sprint);
        }
        return result.stream()
                .sorted(Comparator.comparing(ProjectSprint::getSprintNo))
                .collect(Collectors.toList());
    }

    private List<ProjectDeliverable> buildDeliverables(ProjectSprint sprint, JsonNode deliverablesNode) {
        List<ProjectDeliverable> deliverables = new ArrayList<>();
        int index = 0;
        for (JsonNode node : deliverablesNode) {
            ProjectDeliverable deliverable = new ProjectDeliverable();
            deliverable.setSprint(sprint);
            deliverable.setType(node.path("type").asText("STATEMENT_OF_WORK"));
            deliverable.setTitle(node.path("title").asText("Deliverable " + (index + 1)));
            deliverable.setDescription(node.path("description").asText(""));
            deliverable.setWeight(node.path("weight").asInt(0));
            deliverable.setFileUploadDeliverable(node.path("fileUploadDeliverable").asBoolean(false));
            deliverable.setAutoAddToAllSprints(node.path("autoAddToAllSprints").asBoolean(false));
            deliverable.setRubrics(buildDeliverableRubrics(deliverable, node.path("rubrics")));
            deliverables.add(deliverable);
            index++;
        }
        return deliverables;
    }

    private List<ProjectDeliverableRubric> buildDeliverableRubrics(ProjectDeliverable deliverable, JsonNode rubricsNode) {
        List<ProjectDeliverableRubric> rubrics = new ArrayList<>();
        int displayOrder = 1;
        for (JsonNode node : rubricsNode) {
            ProjectDeliverableRubric rubric = new ProjectDeliverableRubric();
            rubric.setDeliverable(deliverable);
            rubric.setTitle(node.path("title").asText("Rubric " + displayOrder));
            rubric.setCriteriaType(node.path("criteriaType").asText("SOFT").toUpperCase(Locale.ROOT));
            rubric.setDisplayOrder(displayOrder++);
            rubrics.add(rubric);
        }
        return rubrics;
    }

    private List<ProjectEvaluation> buildEvaluations(ProjectSprint sprint, JsonNode evaluationsNode) {
        List<ProjectEvaluation> evaluations = new ArrayList<>();
        int index = 0;
        for (JsonNode node : evaluationsNode) {
            ProjectEvaluation evaluation = new ProjectEvaluation();
            evaluation.setSprint(sprint);
            evaluation.setTitle(node.path("title").asText("Evaluation " + (index + 1)));
            evaluation.setWeight(node.path("weight").asInt(0));
            evaluation.setAutoAddToAllSprints(node.path("autoAddToAllSprints").asBoolean(false));
            evaluation.setRubrics(buildEvaluationRubrics(evaluation, node.path("rubrics")));
            evaluations.add(evaluation);
            index++;
        }
        return evaluations;
    }

    private List<ProjectEvaluationRubric> buildEvaluationRubrics(ProjectEvaluation evaluation, JsonNode rubricsNode) {
        List<ProjectEvaluationRubric> rubrics = new ArrayList<>();
        int displayOrder = 1;
        for (JsonNode node : rubricsNode) {
            ProjectEvaluationRubric rubric = new ProjectEvaluationRubric();
            rubric.setEvaluation(evaluation);
            rubric.setTitle(node.path("title").asText("Rubric " + displayOrder));
            rubric.setCriteriaType(node.path("criteriaType").asText("SOFT").toUpperCase(Locale.ROOT));
            rubric.setDisplayOrder(displayOrder++);
            rubrics.add(rubric);
        }
        return rubrics;
    }

    private LocalDate parseDateOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String value = node.asText("");
        if (value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private ProjectSummary toSummary(Project project) {
        ProjectSummary summary = new ProjectSummary();
        summary.setProjectId(project.getId());
        ProjectTemplate template = project.getTemplate();
        summary.setTemplateId(template != null ? template.getId() : null);
        summary.setTitle(project.getTitle());
        summary.setTerm(project.getTerm());
        summary.setStatus(project.getStatus().name());
        summary.setCreatedAt(project.getCreatedAt());
        summary.setActiveGroupId(project.getGroupId());
        summary.setRepoFullName(project.getRepoFullName());
        summary.setRepoHtmlUrl(project.getRepoHtmlUrl());
        summary.setRepoDefaultBranch(project.getRepoDefaultBranch());
        summary.setRepoProviderId(project.getRepoProviderId());
        return summary;
    }

    private ProjectDetail toDetail(
            Project project,
            Map<Long, List<ProjectDeliverableRubric>> deliverableRubricsFromDb,
            Map<Long, List<ProjectEvaluationRubric>> evaluationRubricsFromDb) {
        ProjectDetail detail = new ProjectDetail();
        detail.setProjectId(project.getId());
        ProjectTemplate template = project.getTemplate();
        detail.setTemplateId(template != null ? template.getId() : null);
        detail.setTitle(project.getTitle());
        detail.setTerm(project.getTerm());
        detail.setStatus(project.getStatus().name());
        detail.setCreatedByUserId(project.getCreatedByUserId());
        detail.setCreatedAt(project.getCreatedAt());
        detail.setUpdatedAt(project.getUpdatedAt());
        detail.setActiveGroupId(project.getGroupId());
        detail.setRepoFullName(project.getRepoFullName());
        detail.setRepoHtmlUrl(project.getRepoHtmlUrl());
        detail.setRepoDefaultBranch(project.getRepoDefaultBranch());
        detail.setRepoProviderId(project.getRepoProviderId());
        detail.setJiraProjectKey(project.getJiraProjectKey());
        detail.setJiraProjectUrl(resolveJiraProjectBrowseUrl(project));
        List<JiraGithubMatchDto> syncRows = loadJiraGithubMatches(project.getId());
        detail.setJiraGithubMatches(syncRows);
        detail.setGithubBranches(loadGithubBranches(project, syncRows));
        detail.setSprints(project.getSprints().stream()
                .sorted(Comparator.comparing(ProjectSprint::getSprintNo))
                .map(s -> toSprintDto(s, deliverableRubricsFromDb, evaluationRubricsFromDb))
                .toList());
        ProjectGithubIssueSyncService.StoryPointValidationResult validation =
                projectGithubIssueSyncService.validateStoryPoints(project.getId());
        StoryPointValidationDto validationDto = new StoryPointValidationDto();
        validationDto.setMatched(validation.matched());
        validationDto.setExpected(validation.expected());
        validationDto.setActual(validation.actual());
        detail.setStoryPointValidation(validationDto);
        detail.setGithubIssues(validation.issues().stream().map(this::toGithubIssueDto).toList());
        return detail;
    }

    private List<JiraGithubMatchDto> loadJiraGithubMatches(Long projectId) {
        if (projectId == null) return List.of();
        return projectIssueSyncSnapshotRepository.findByProject_IdOrderByIssueKeyAsc(projectId).stream()
                .map(snapshot -> {
                    JiraGithubMatchDto dto = new JiraGithubMatchDto();
                    dto.setBranchName(snapshot.getBranchName());
                    dto.setIssueKey(snapshot.getIssueKey());
                    dto.setIssueTitle(snapshot.getIssueTitle());
                    dto.setIssueDescription(snapshot.getIssueDescription());
                    dto.setStoryPoints(snapshot.getStoryPoints());
                    dto.setSprintNo(snapshot.getSprintNo());
                    dto.setJiraAssignee(snapshot.getAssignee());
                    dto.setPrNumber(snapshot.getPrNumber());
                    dto.setPrMerged(snapshot.getPrMerged());
                    return dto;
                })
                .toList();
    }

    private List<String> loadGithubBranches(Project project, List<JiraGithubMatchDto> syncRows) {
        Set<String> branches = new java.util.LinkedHashSet<>();
        String repoDefaultBranch = project != null ? project.getRepoDefaultBranch() : null;
        if (repoDefaultBranch != null && !repoDefaultBranch.isBlank()) {
            branches.add(repoDefaultBranch.trim());
        }
        for (JiraGithubMatchDto row : syncRows) {
            if (row.getBranchName() != null && !row.getBranchName().isBlank()) {
                branches.add(row.getBranchName().trim());
            }
        }
        if (project != null && project.getRepoFullName() != null && !project.getRepoFullName().isBlank()) {
            branches.addAll(fetchAllGithubBranches(project));
        }
        return List.copyOf(branches);
    }

    private List<String> fetchAllGithubBranches(Project project) {
        try {
            String encryptedPat = resolveTemplateCreatorPat(project);
            if (encryptedPat == null || encryptedPat.isBlank()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            int page = 1;
            while (true) {
                String endpoint = "https://api.github.com/repos/" + project.getRepoFullName()
                        + "/branches?per_page=100&page=" + page;
                String body = secureOutboundApiService.executeGitHubApiCall(
                        encryptedPat, endpoint, null, HttpMethod.GET).getBody();
                JsonNode node = objectMapper.readTree(body);
                if (!node.isArray() || node.isEmpty()) {
                    break;
                }
                for (JsonNode branchNode : node) {
                    String branchName = branchNode.path("name").asText(null);
                    if (branchName != null && !branchName.isBlank()) {
                        result.add(branchName.trim());
                    }
                }
                if (node.size() < 100) {
                    break;
                }
                page++;
            }
            return result;
        } catch (Exception ex) {
            log.warn("Could not fetch full GitHub branch list for project {}: {}", project.getId(), ex.getMessage());
            return List.of();
        }
    }

    private String resolveJiraProjectBrowseUrl(Project project) {
        try {
            if (project == null || project.getJiraProjectKey() == null || project.getJiraProjectKey().isBlank()) {
                return null;
            }
            String raw = project.getTemplate() != null ? project.getTemplate().getTemplateJson() : null;
            if (raw == null || raw.isBlank()) return null;
            String site = objectMapper.readTree(raw).path("jiraSiteUrl").asText(null);
            if (site == null || site.isBlank()) return null;
            String normalized = site.trim();
            if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
                normalized = "https://" + normalized;
            }
            if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
            return normalized + "/browse/" + project.getJiraProjectKey();
        } catch (Exception ex) {
            return null;
        }
    }

    private SprintDto toSprintDto(
            ProjectSprint sprint,
            Map<Long, List<ProjectDeliverableRubric>> deliverableRubricsFromDb,
            Map<Long, List<ProjectEvaluationRubric>> evaluationRubricsFromDb) {
        SprintDto dto = new SprintDto();
        dto.setSprintNo(sprint.getSprintNo());
        dto.setTitle(sprint.getTitle());
        dto.setStartDate(sprint.getStartDate());
        dto.setEndDate(sprint.getEndDate());
        dto.setDeliverables(
                sprint.getDeliverables().stream().map(d -> toDeliverableDto(d, deliverableRubricsFromDb)).toList());
        dto.setEvaluations(
                sprint.getEvaluations().stream().map(e -> toEvaluationDto(e, evaluationRubricsFromDb)).toList());
        return dto;
    }

    private DeliverableDto toDeliverableDto(
            ProjectDeliverable deliverable, Map<Long, List<ProjectDeliverableRubric>> rubricsFromDb) {
        DeliverableDto dto = new DeliverableDto();
        dto.setId(deliverable.getId());
        dto.setType(deliverable.getType());
        dto.setTitle(deliverable.getTitle());
        dto.setDescription(deliverable.getDescription());
        dto.setWeight(deliverable.getWeight());
        dto.setFileUploadDeliverable(deliverable.isFileUploadDeliverable());
        dto.setAutoAddToAllSprints(deliverable.isAutoAddToAllSprints());
        List<ProjectDeliverableRubric> rubricEntities;
        if (rubricsFromDb != null && deliverable.getId() != null) {
            rubricEntities = rubricsFromDb.getOrDefault(deliverable.getId(), List.of());
        } else {
            rubricEntities = deliverable.getRubrics();
        }
        dto.setRubrics(rubricEntities.stream()
                .sorted(Comparator.comparing(ProjectDeliverableRubric::getDisplayOrder))
                .map(this::toRubricDto)
                .toList());
        return dto;
    }

    private EvaluationDto toEvaluationDto(
            ProjectEvaluation evaluation, Map<Long, List<ProjectEvaluationRubric>> rubricsFromDb) {
        EvaluationDto dto = new EvaluationDto();
        dto.setId(evaluation.getId());
        dto.setTitle(evaluation.getTitle());
        dto.setWeight(evaluation.getWeight());
        dto.setAutoAddToAllSprints(evaluation.isAutoAddToAllSprints());
        List<ProjectEvaluationRubric> rubricEntities;
        if (rubricsFromDb != null && evaluation.getId() != null) {
            rubricEntities = rubricsFromDb.getOrDefault(evaluation.getId(), List.of());
        } else {
            rubricEntities = evaluation.getRubrics();
        }
        dto.setRubrics(rubricEntities.stream()
                .sorted(Comparator.comparing(ProjectEvaluationRubric::getDisplayOrder))
                .map(this::toRubricDto)
                .toList());
        return dto;
    }

    private RubricDto toRubricDto(ProjectDeliverableRubric rubric) {
        RubricDto dto = new RubricDto();
        dto.setId(rubric.getId());
        dto.setTitle(rubric.getTitle());
        dto.setCriteriaType(rubric.getCriteriaType());
        return dto;
    }

    private RubricDto toRubricDto(ProjectEvaluationRubric rubric) {
        RubricDto dto = new RubricDto();
        dto.setId(rubric.getId());
        dto.setTitle(rubric.getTitle());
        dto.setCriteriaType(rubric.getCriteriaType());
        return dto;
    }

    private CommitteeDto toCommitteeDto(ProjectCommittee committee) {
        CommitteeDto dto = new CommitteeDto();
        dto.setCommitteeId(committee.getId());
        dto.setProjectId(committee.getProject().getId());
        dto.setName(committee.getName());
        dto.setProfessors(committee.getProfessors().stream()
                .map(ProjectCommitteeProfessor::getProfessor)
                .filter(Objects::nonNull)
                .filter(user -> user.getRole() == Role.PROFESSOR || user.getRole() == Role.COORDINATOR)
                .map(this::toProfessorOptionDto)
                .toList());
        return dto;
    }

    private ProfessorOptionDto toProfessorOptionDto(User professor) {
        ProfessorOptionDto dto = new ProfessorOptionDto();
        dto.setUserId(professor.getId());
        dto.setFullName(professor.getFullName());
        dto.setEmail(professor.getEmail());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<AdvisorLiveGradeRow> getAdvisorLiveGrades(Long advisorUserId) {
        if (advisorUserId == null) return List.of();
        return projectRepository.findAll().stream()
                .filter(p -> p.getGroupId() != null)
                .map(p -> {
                    AdvisorLiveGradeRow row = new AdvisorLiveGradeRow();
                    row.setProjectId(p.getId());
                    row.setProjectTitle(p.getTitle());
                    row.setGroupId(p.getGroupId());
                    ProjectGradingSummaryDto summary = pdfGradingEngineService.buildSummary(p, p.getGroupId());
                    row.setCumulativeTeamGrade(summary != null ? summary.getCumulativeTeamGrade() : null);
                    row.setAdjustedIndividualGrade(summary != null ? summary.getAdjustedIndividualGrade() : null);
                    row.setOverallSuccessGrade(summary != null ? summary.getOverallSuccessGrade() : null);
                    return row;
                })
                .toList();
    }

    private GithubIssueDto toGithubIssueDto(ProjectGithubIssue issue) {
        GithubIssueDto dto = new GithubIssueDto();
        dto.setIssueNumber(issue.getIssueNumber());
        dto.setTitle(issue.getTitle());
        dto.setState(issue.getState());
        dto.setAssignee(issue.getAssignee());
        dto.setSprintNo(issue.getSprint() != null ? issue.getSprint().getSprintNo() : null);
        dto.setStoryPoints(issue.getStoryPoints());
        return dto;
    }

    private void createGithubRepoOnAssignmentRequired(Project project, Long groupId, Long assignedByUserId) {
        boolean createFlag = shouldCreateGithubRepo(project);
        if (!createFlag) {
            throw new IllegalArgumentException(
                    "Template is not configured for GitHub repo creation (createGithubRepo=false).");
        }
        if (project.getRepoFullName() != null) {
            log.info("GitHub repo create skipped: repo already exists on project (projectId={}, repoFullName={})",
                    project.getId(), project.getRepoFullName());
            return;
        }
        Long safeGroupId = Objects.requireNonNull(groupId, "groupId required");
        String encryptedPat = resolveTemplateCreatorPat(project);
        if (encryptedPat == null || encryptedPat.isBlank()) {
            throw new IllegalArgumentException("Template creator coordinator GitHub PAT is missing. Save PAT in My Profile.");
        }
        try {
            String ownerLogin = resolveGithubOwnerLogin(encryptedPat);
            if (ownerLogin == null || ownerLogin.isBlank()) {
                throw new IllegalStateException("Could not resolve GitHub owner login from PAT.");
            }
            String repoName = buildRepoName(project, safeGroupId);
            String body = objectMapper.createObjectNode()
                    .put("name", repoName)
                    .put("private", true)
                    .put("description", "SeniorApp project " + project.getId())
                    .toString();
            JsonNode repoNode = objectMapper.readTree(
                    secureOutboundApiService.executeGitHubApiCall(
                            encryptedPat, "https://api.github.com/user/repos", body, HttpMethod.POST).getBody());
            project.setRepoFullName(repoNode.path("full_name").asText(ownerLogin + "/" + repoName));
            project.setRepoHtmlUrl(repoNode.path("html_url").asText(null));
            project.setRepoDefaultBranch(repoNode.path("default_branch").asText("main"));
            project.setRepoProviderId(repoNode.path("id").asText(null));
            projectRepository.save(project);
            inviteGroupStudentsToGithubRepo(project, safeGroupId, encryptedPat);
            log.info("GitHub repo created and persisted (projectId={}, repoFullName={})",
                    project.getId(), project.getRepoFullName());
        } catch (Exception e) {
            throw new IllegalStateException("GitHub repo creation failed: " + e.getMessage(), e);
        }
    }

    private boolean shouldCreateGithubRepo(Project project) {
        try {
            String raw = project.getTemplate() != null ? project.getTemplate().getTemplateJson() : null;
            if (raw == null || raw.isBlank()) return false;
            return objectMapper.readTree(raw).path("createGithubRepo").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldCreateJiraWorkspace(Project project) {
        try {
            String raw = project.getTemplate() != null ? project.getTemplate().getTemplateJson() : null;
            if (raw == null || raw.isBlank()) return false;
            return objectMapper.readTree(raw).path("createJiraWorkspace").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveGithubOwnerLogin(String encryptedPat) throws Exception {
        String body = secureOutboundApiService.executeGitHubApiCall(
                encryptedPat, "https://api.github.com/user", null, HttpMethod.GET).getBody();
        return objectMapper.readTree(body).path("login").asText(null);
    }

    private String resolveTemplateCreatorPat(Project project) {
        ProjectTemplate template = project.getTemplate();
        if (template == null || template.getCreatedByUserId() == null) return null;
        Long creatorUserId = Objects.requireNonNull(template.getCreatedByUserId(), "template createdByUserId required");
        User creator = userRepository.findById(creatorUserId).orElse(null);
        if (creator == null) return null;
        String pat = creator.getGithubPatEncrypted();
        if (pat != null && !pat.isBlank()) {
            return pat;
        }
        return null;
    }

    private String resolveTemplateCreatorJiraToken(Project project) {
        ProjectTemplate template = project.getTemplate();
        Long creatorUserId = template != null ? template.getCreatedByUserId() : null;
        if (creatorUserId == null) return null;
        User creator = userRepository.findById(creatorUserId).orElse(null);
        return creator != null ? creator.getJiraApiTokenEncrypted() : null;
    }

    private String resolveTemplateJiraSiteUrl(Project project) {
        try {
            String raw = project.getTemplate() != null ? project.getTemplate().getTemplateJson() : null;
            if (raw == null || raw.isBlank()) return null;
            return objectMapper.readTree(raw).path("jiraSiteUrl").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private void createJiraWorkspaceOnAssignmentRequired(Project project, Long groupId) {
        if (!shouldCreateJiraWorkspace(project)) {
            return;
        }
        if (project.getJiraProjectKey() != null && !project.getJiraProjectKey().isBlank()) {
            return;
        }
        String templateJiraSiteUrl = resolveTemplateJiraSiteUrl(project);
        String encryptedApiToken = resolveTemplateCreatorJiraToken(project);
        if (templateJiraSiteUrl == null || templateJiraSiteUrl.isBlank()
                || encryptedApiToken == null || encryptedApiToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Template Jira domain or Jira OAuth connection is missing.");
        }
        try {
            JiraProvisioningService.JiraProvisioningResult result =
                    jiraProvisioningService.provisionProject(project, groupId, templateJiraSiteUrl, encryptedApiToken);
            project.setJiraProjectKey(result.getProjectKey());
            project.setJiraProjectId(result.getProjectId());
            project.setJiraBoardId(result.getBoardId());
            projectRepository.save(project);
            log.info("Jira workspace created and persisted (projectId={}, jiraProjectKey={})",
                    project.getId(), project.getJiraProjectKey());
        } catch (Exception e) {
            throw new IllegalStateException("Jira workspace creation failed: " + e.getMessage(), e);
        }
    }

    private String buildRepoName(Project project, Long groupId) {
        String base = (project.getTitle() == null ? "project" : project.getTitle())
                .trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "project";
        return base + "-g" + groupId + "-p" + project.getId();
    }

    private void inviteGroupStudentsToGithubRepo(Project project, Long groupId, String encryptedPat) {
        if (project == null || project.getRepoFullName() == null || project.getRepoFullName().isBlank()) {
            return;
        }
        List<UserGroupMember> acceptedMembers =
                userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
        for (UserGroupMember membership : acceptedMembers) {
            User member = membership.getUser();
            if (member == null || member.getRole() != Role.STUDENT) {
                continue;
            }
            String githubUsername = member.getGithubUsername();
            if (githubUsername == null || githubUsername.isBlank()) {
                log.info("GitHub collaborator invite skipped: student has no linked github username (userId={})",
                        member.getId());
                continue;
            }
            try {
                String endpoint = "https://api.github.com/repos/" + project.getRepoFullName()
                        + "/collaborators/" + URLEncoder.encode(githubUsername, StandardCharsets.UTF_8);
                String body = objectMapper.createObjectNode()
                        .put("permission", "push")
                        .toString();
                secureOutboundApiService.executeGitHubApiCall(encryptedPat, endpoint, body, HttpMethod.PUT);
                log.info("GitHub collaborator invited (repo={}, username={})", project.getRepoFullName(), githubUsername);
            } catch (Exception ex) {
                // Best effort: repo creation should still succeed even if one invite fails.
                log.warn("GitHub collaborator invite failed (repo={}, username={}): {}",
                        project.getRepoFullName(), githubUsername, ex.getMessage());
            }
        }
    }
}
