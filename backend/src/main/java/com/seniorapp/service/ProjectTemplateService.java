package com.seniorapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seniorapp.dto.projecttemplate.CreateProjectTemplateRequest;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.ProjectTemplateDetail;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.ProjectTemplateSummary;
import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.repository.ProjectTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
public class ProjectTemplateService {

    private static final Map<String, Integer> FIXED_GRADE_POINTS = Map.of(
            "S", 100,
            "A", 100,
            "B", 80,
            "C", 60,
            "D", 50,
            "F", 0
    );

    private final ProjectTemplateRepository repository;
    private final ObjectMapper objectMapper;

    public ProjectTemplateService(ProjectTemplateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long createTemplate(CreateProjectTemplateRequest request, Long createdByUserId) {
        validateRequest(request);
        if (createdByUserId == null) {
            throw new IllegalArgumentException("Authenticated user id is required.");
        }
        LocalDate projectStartDate = parseProjectStartDate(request.getProjectStartDate());

        Map<String, Object> payload = new LinkedHashMap<>();
        JsonNode normalizedSprints = removeEvaluationTypeField(request.getSprints());
        payload.put("name", request.getName());
        payload.put("description", request.getDescription());
        payload.put("term", request.getTerm());
        payload.put("projectStartDate", request.getProjectStartDate());
        payload.put("sprints", normalizedSprints);
        payload.put("fixedGradePoints", FIXED_GRADE_POINTS);

        ProjectTemplate template = new ProjectTemplate();
        template.setName(request.getName().trim());
        template.setDescription(request.getDescription().trim());
        template.setTerm(request.getTerm().trim());
        template.setCreatedBy(String.valueOf(createdByUserId));
        template.setCreatedByUserId(createdByUserId);
        template.setProjectStartDate(projectStartDate);
        template.setVersion(1);
        template.setActive(true);
        template.setTemplateJson(serialize(payload));

        return Objects.requireNonNull(repository.save(template).getId(), "Persisted template id is null.");
    }

    @Transactional(readOnly = true)
    public List<ProjectTemplateSummary> listTemplates() {
        return repository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectTemplateDetail getTemplate(Long templateId) {
        Long safeTemplateId = Objects.requireNonNull(templateId, "templateId cannot be null.");
        ProjectTemplate template = repository.findById(safeTemplateId)
                .orElseThrow(() -> new NoSuchElementException("Project template not found: " + templateId));
        return toDetail(template);
    }

    private void validateRequest(CreateProjectTemplateRequest request) {
        parseProjectStartDate(request.getProjectStartDate());
        JsonNode sprints = request.getSprints();
        if (!sprints.isArray() || sprints.isEmpty()) {
            throw new IllegalArgumentException("sprints must be a non-empty array.");
        }

        int projectDeliverableWeightTotal = 0;
        for (JsonNode sprint : sprints) {
            JsonNode sprintNo = sprint.get("sprintNo");
            if (sprintNo == null || !sprintNo.canConvertToInt()) {
                throw new IllegalArgumentException("Each sprint must include an integer sprintNo.");
            }

            JsonNode deliverables = sprint.get("deliverables");
            if (deliverables == null || !deliverables.isArray()) {
                throw new IllegalArgumentException("Each sprint must include a deliverables array.");
            }

            Set<String> deliverableTitles = new HashSet<>();
            for (JsonNode deliverable : deliverables) {
                String deliverableTitle = normalizeName(deliverable.get("title"));
                if (deliverableTitle.isEmpty()) {
                    throw new IllegalArgumentException("Deliverable title cannot be empty.");
                }
                if (!deliverableTitles.add(deliverableTitle)) {
                    throw new IllegalArgumentException("Duplicate deliverable title is not allowed within a sprint.");
                }
                JsonNode weight = deliverable.get("weight");
                if (weight != null && weight.isNumber() && weight.asInt() < 0) {
                    throw new IllegalArgumentException("Deliverable weight cannot be negative.");
                }
                projectDeliverableWeightTotal += weight != null && weight.isNumber() ? weight.asInt() : 0;

                JsonNode deliverableRubrics = deliverable.get("rubrics");
                if (deliverableRubrics == null || !deliverableRubrics.isArray() || deliverableRubrics.isEmpty()) {
                    throw new IllegalArgumentException("Each deliverable must include a non-empty rubrics array.");
                }
                Set<String> deliverableRubricTitles = new HashSet<>();
                for (JsonNode rubric : deliverableRubrics) {
                    String rubricTitle = normalizeName(rubric.get("title"));
                    if (rubricTitle.isEmpty()) {
                        throw new IllegalArgumentException("Deliverable rubric title cannot be empty.");
                    }
                    if (!deliverableRubricTitles.add(rubricTitle)) {
                        throw new IllegalArgumentException("Duplicate deliverable rubric title is not allowed.");
                    }
                }
            }

            JsonNode sprintEvaluations = sprint.get("evaluations");
            if (sprintEvaluations == null || !sprintEvaluations.isArray() || sprintEvaluations.isEmpty()) {
                throw new IllegalArgumentException("Each sprint must include a non-empty evaluations array.");
            }

            Set<String> evaluationTitles = new HashSet<>();
            int sprintEvaluationTotal = 0;
            for (JsonNode evaluation : sprintEvaluations) {
                String evaluationTitle = normalizeName(evaluation.get("title"));
                if (evaluationTitle.isEmpty()) {
                    throw new IllegalArgumentException("Evaluation title cannot be empty.");
                }
                if (!evaluationTitles.add(evaluationTitle)) {
                    throw new IllegalArgumentException("Duplicate evaluation title is not allowed within a sprint.");
                }
                JsonNode evaluationWeight = evaluation.get("weight");
                if (evaluationWeight != null && evaluationWeight.isNumber() && evaluationWeight.asInt() < 0) {
                    throw new IllegalArgumentException("Evaluation weight cannot be negative.");
                }
                sprintEvaluationTotal += evaluationWeight != null && evaluationWeight.isNumber() ? evaluationWeight.asInt() : 0;
                JsonNode evaluationRubrics = evaluation.get("rubrics");
                if (evaluationRubrics == null || !evaluationRubrics.isArray() || evaluationRubrics.isEmpty()) {
                    throw new IllegalArgumentException("Each sprint evaluation must include a non-empty rubrics array.");
                }
                Set<String> evaluationRubricTitles = new HashSet<>();
                for (JsonNode rubric : evaluationRubrics) {
                    String rubricTitle = normalizeName(rubric.get("title"));
                    if (rubricTitle.isEmpty()) {
                        throw new IllegalArgumentException("Evaluation rubric title cannot be empty.");
                    }
                    if (!evaluationRubricTitles.add(rubricTitle)) {
                        throw new IllegalArgumentException("Duplicate evaluation rubric title is not allowed.");
                    }
                }
            }
            if (sprintEvaluationTotal != 100) {
                throw new IllegalArgumentException("Each sprint's evaluation weights must total exactly 100.");
            }
        }
        if (projectDeliverableWeightTotal != 100) {
            throw new IllegalArgumentException("Project deliverable total weight must be exactly 100.");
        }
    }

    private String normalizeName(JsonNode node) {
        if (node == null || node.isNull()) return "";
        return node.asText("").trim().toLowerCase();
    }

    private LocalDate parseProjectStartDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("projectStartDate must be in ISO format: YYYY-MM-DD");
        }
    }

    private JsonNode removeEvaluationTypeField(JsonNode sprintsNode) {
        ArrayNode clonedSprints = objectMapper.createArrayNode();
        for (JsonNode sprintNode : sprintsNode) {
            ObjectNode sprintCopy = sprintNode.deepCopy();
            JsonNode evaluationsNode = sprintCopy.get("evaluations");
            if (evaluationsNode != null && evaluationsNode.isArray()) {
                ArrayNode normalizedEvaluations = objectMapper.createArrayNode();
                for (JsonNode evaluationNode : evaluationsNode) {
                    ObjectNode evaluationCopy = evaluationNode.deepCopy();
                    evaluationCopy.remove("type");
                    normalizedEvaluations.add(evaluationCopy);
                }
                sprintCopy.set("evaluations", normalizedEvaluations);
            }
            clonedSprints.add(sprintCopy);
        }
        return clonedSprints;
    }

    private ProjectTemplateSummary toSummary(ProjectTemplate template) {
        ProjectTemplateSummary summary = new ProjectTemplateSummary();
        summary.setTemplateId(template.getId());
        summary.setName(template.getName());
        summary.setDescription(template.getDescription());
        summary.setTerm(template.getTerm());
        summary.setCreatedByUserId(template.getCreatedByUserId());
        summary.setProjectStartDate(template.getProjectStartDate());
        summary.setVersion(template.getVersion());
        summary.setActive(template.isActive());
        summary.setCreatedAt(template.getCreatedAt());
        summary.setUpdatedAt(template.getUpdatedAt());
        return summary;
    }

    private ProjectTemplateDetail toDetail(ProjectTemplate template) {
        ProjectTemplateDetail detail = new ProjectTemplateDetail();
        detail.setTemplateId(template.getId());
        detail.setName(template.getName());
        detail.setDescription(template.getDescription());
        detail.setTerm(template.getTerm());
        detail.setCreatedByUserId(template.getCreatedByUserId());
        detail.setProjectStartDate(template.getProjectStartDate());
        detail.setVersion(template.getVersion());
        detail.setActive(template.isActive());
        detail.setPayload(deserialize(template.getTemplateJson()));
        detail.setCreatedAt(template.getCreatedAt());
        detail.setUpdatedAt(template.getUpdatedAt());
        return detail;
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize project template payload.", e);
        }
    }

    private JsonNode deserialize(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored project template payload is invalid JSON.", e);
        }
    }
}
