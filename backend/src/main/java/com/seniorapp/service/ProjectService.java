package com.seniorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.project.ProjectDtos.*;
import com.seniorapp.entity.*;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectGroupAssignmentRepository assignmentRepository;
    private final ObjectMapper objectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectGroupAssignmentRepository assignmentRepository,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.projectTemplateRepository = projectTemplateRepository;
        this.assignmentRepository = assignmentRepository;
        this.objectMapper = objectMapper;
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
        project.setGroupId(groupId);
        projectRepository.save(project);

        ProjectGroupAssignment assignment = new ProjectGroupAssignment();
        assignment.setProject(project);
        assignment.setGroupId(groupId);
        assignment.setAssignedByUserId(assignedByUserId);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setActive(true);
        ProjectGroupAssignment saved = assignmentRepository.save(assignment);

        return new AssignmentResponse("success", safeProjectId, saved.getGroupId(), saved.getAssignedAt());
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> listProjects(String term, Long templateId, Long groupId) {
        return projectRepository.findAll().stream()
                .filter(project -> term == null || term.isBlank() || project.getTerm().equalsIgnoreCase(term.trim()))
                .filter(project -> templateId == null || Objects.equals(project.getTemplate().getId(), templateId))
                .filter(project -> {
                    if (groupId == null) return true;
                    return Objects.equals(project.getGroupId(), groupId);
                })
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetail getProjectDetail(Long projectId) {
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId is required.");
        Project project = projectRepository.findById(safeProjectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
        return toDetail(project);
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
        summary.setTemplateId(project.getTemplate().getId());
        summary.setTitle(project.getTitle());
        summary.setTerm(project.getTerm());
        summary.setStatus(project.getStatus().name());
        summary.setCreatedAt(project.getCreatedAt());
        summary.setActiveGroupId(project.getGroupId());
        return summary;
    }

    private ProjectDetail toDetail(Project project) {
        ProjectDetail detail = new ProjectDetail();
        detail.setProjectId(project.getId());
        detail.setTemplateId(project.getTemplate().getId());
        detail.setTitle(project.getTitle());
        detail.setTerm(project.getTerm());
        detail.setStatus(project.getStatus().name());
        detail.setCreatedByUserId(project.getCreatedByUserId());
        detail.setCreatedAt(project.getCreatedAt());
        detail.setUpdatedAt(project.getUpdatedAt());
        detail.setActiveGroupId(project.getGroupId());
        detail.setSprints(project.getSprints().stream()
                .sorted(Comparator.comparing(ProjectSprint::getSprintNo))
                .map(this::toSprintDto)
                .toList());
        return detail;
    }

    private SprintDto toSprintDto(ProjectSprint sprint) {
        SprintDto dto = new SprintDto();
        dto.setSprintNo(sprint.getSprintNo());
        dto.setTitle(sprint.getTitle());
        dto.setStartDate(sprint.getStartDate());
        dto.setEndDate(sprint.getEndDate());
        dto.setDeliverables(sprint.getDeliverables().stream().map(this::toDeliverableDto).toList());
        dto.setEvaluations(sprint.getEvaluations().stream().map(this::toEvaluationDto).toList());
        return dto;
    }

    private DeliverableDto toDeliverableDto(ProjectDeliverable deliverable) {
        DeliverableDto dto = new DeliverableDto();
        dto.setType(deliverable.getType());
        dto.setTitle(deliverable.getTitle());
        dto.setDescription(deliverable.getDescription());
        dto.setWeight(deliverable.getWeight());
        dto.setFileUploadDeliverable(deliverable.isFileUploadDeliverable());
        dto.setAutoAddToAllSprints(deliverable.isAutoAddToAllSprints());
        dto.setRubrics(deliverable.getRubrics().stream()
                .sorted(Comparator.comparing(ProjectDeliverableRubric::getDisplayOrder))
                .map(this::toRubricDto)
                .toList());
        return dto;
    }

    private EvaluationDto toEvaluationDto(ProjectEvaluation evaluation) {
        EvaluationDto dto = new EvaluationDto();
        dto.setTitle(evaluation.getTitle());
        dto.setWeight(evaluation.getWeight());
        dto.setAutoAddToAllSprints(evaluation.isAutoAddToAllSprints());
        dto.setRubrics(evaluation.getRubrics().stream()
                .sorted(Comparator.comparing(ProjectEvaluationRubric::getDisplayOrder))
                .map(this::toRubricDto)
                .toList());
        return dto;
    }

    private RubricDto toRubricDto(ProjectDeliverableRubric rubric) {
        RubricDto dto = new RubricDto();
        dto.setTitle(rubric.getTitle());
        dto.setCriteriaType(rubric.getCriteriaType());
        return dto;
    }

    private RubricDto toRubricDto(ProjectEvaluationRubric rubric) {
        RubricDto dto = new RubricDto();
        dto.setTitle(rubric.getTitle());
        dto.setCriteriaType(rubric.getCriteriaType());
        return dto;
    }
}
