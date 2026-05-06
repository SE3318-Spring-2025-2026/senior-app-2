package com.seniorapp.service;

import com.seniorapp.entity.*;
import com.seniorapp.repository.DeliverableSubmissionFileRepository;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.ProjectDeliverableRepository;
import com.seniorapp.repository.UserGroupMemberRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deliverable submission: text + multi-file upload per (deliverable, group).
 * New files use {@link DeliverableSubmissionFile}; legacy single file_path is still supported for download.
 */
@Service
public class DeliverableSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(DeliverableSubmissionService.class);

    public record FileDownload(Resource resource, String filename) {}

    private final DeliverableSubmissionRepository submissionRepository;
    private final DeliverableSubmissionFileRepository submissionFileRepository;
    private final ProjectDeliverableRepository deliverableRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;

    @Value("${app.storage.base-path:./storage}")
    private String storageBasePath;

    public DeliverableSubmissionService(
            DeliverableSubmissionRepository submissionRepository,
            DeliverableSubmissionFileRepository submissionFileRepository,
            ProjectDeliverableRepository deliverableRepository,
            UserGroupMemberRepository userGroupMemberRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.submissionFileRepository = submissionFileRepository;
        this.deliverableRepository = deliverableRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
    }

    /**
     * Öğrenci teslim etmeden notlandırma için (koordinatör/profesör) boş teslim kaydı oluşturur veya mevcudu döner.
     * Öğrenci yüklemelerindeki sprint süresi kuralı burada uygulanmaz.
     */
    @Transactional
    public DeliverableSubmission ensureGradingPlaceholder(Long deliverableId, Long groupId, Long actingUserId) {
        Optional<DeliverableSubmission> existing = submissionRepository.findByDeliverableIdAndGroupId(deliverableId, groupId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ProjectDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new NoSuchElementException("Deliverable bulunamadı: " + deliverableId));
        DeliverableSubmission s = new DeliverableSubmission();
        s.setDeliverable(deliverable);
        s.setGroupId(groupId);
        s.setSubmittedByUserId(actingUserId);
        s.setSubmissionType(SubmissionType.TEXT_EDITOR);
        s.setTextContent("");
        s.setStatus(SubmissionStatus.DRAFT);
        return submissionRepository.save(s);
    }

    @Transactional
    public DeliverableSubmission submitFile(Long deliverableId, Long groupId, Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz.");
        }

        ProjectDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new NoSuchElementException("Deliverable bulunamadı: " + deliverableId));
        assertSprintAcceptsSubmission(deliverable);

        Long projectId = deliverable.getSprint().getProject().getId();

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "unnamed_file";
        }
        originalFileName = sanitizeStoredOriginalName(originalFileName);

        Path storagePath = Paths.get(
                storageBasePath,
                String.valueOf(projectId),
                String.valueOf(groupId),
                String.valueOf(deliverableId)
        );

        try {
            Files.createDirectories(storagePath);

            String storedName = UUID.randomUUID() + "-" + originalFileName;
            Path targetFile = storagePath.resolve(storedName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("Dosya kaydedildi: {} (boyut: {} byte)", targetFile, file.getSize());

            DeliverableSubmission submission = submissionRepository
                    .findByDeliverableIdAndGroupId(deliverableId, groupId)
                    .orElse(new DeliverableSubmission());

            if (submission.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(submission.getFilePath()));
                } catch (IOException e) {
                    log.warn("Eski legacy dosya silinemedi: {}", e.getMessage());
                }
            }

            submission.setDeliverable(deliverable);
            submission.setGroupId(groupId);
            submission.setSubmittedByUserId(userId);
            submission.setSubmissionType(SubmissionType.FILE_UPLOAD);
            submission.setStatus(SubmissionStatus.SUBMITTED);
            submission.setTextContent(null);
            submission.setFilePath(null);
            submission.setOriginalFileName(null);
            submission.setFileSize(null);

            DeliverableSubmission saved = submissionRepository.save(submission);

            DeliverableSubmissionFile row = new DeliverableSubmissionFile();
            row.setSubmission(saved);
            row.setFilePath(targetFile.toString());
            row.setOriginalFileName(originalFileName);
            row.setFileSize(file.getSize());
            submissionFileRepository.save(row);

            return submissionRepository.findById(saved.getId()).orElse(saved);
        } catch (IOException e) {
            log.error("Dosya kaydetme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Dosya kaydedilemedi: " + e.getMessage());
        }
    }

    @Transactional
    public DeliverableSubmission submitText(Long deliverableId, Long groupId, Long userId, String textContent) {
        if (textContent == null || textContent.isBlank()) {
            throw new IllegalArgumentException("Metin içeriği boş olamaz.");
        }

        ProjectDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new NoSuchElementException("Deliverable bulunamadı: " + deliverableId));
        assertSprintAcceptsSubmission(deliverable);

        DeliverableSubmission submission = submissionRepository
                .findByDeliverableIdAndGroupId(deliverableId, groupId)
                .orElse(new DeliverableSubmission());

        removeAllStoredFilesFromDisk(submission);
        submission.getFiles().clear();

        if (submission.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(submission.getFilePath()));
            } catch (IOException e) {
                log.warn("Legacy dosya silinemedi: {}", e.getMessage());
            }
        }

        submission.setDeliverable(deliverable);
        submission.setGroupId(groupId);
        submission.setSubmittedByUserId(userId);
        submission.setSubmissionType(SubmissionType.TEXT_EDITOR);
        submission.setTextContent(textContent);
        submission.setFilePath(null);
        submission.setOriginalFileName(null);
        submission.setFileSize(null);
        submission.setStatus(SubmissionStatus.SUBMITTED);

        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public Optional<DeliverableSubmission> getSubmission(Long deliverableId, Long groupId) {
        return submissionRepository.findByDeliverableIdAndGroupId(deliverableId, groupId);
    }

    @Transactional(readOnly = true)
    public List<DeliverableSubmission> getSubmissionsByProject(Long projectId, Long groupId) {
        return submissionRepository.findByDeliverable_Sprint_Project_IdAndGroupId(projectId, groupId);
    }

    @Transactional(readOnly = true)
    public Optional<DeliverableSubmission> getSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId);
    }

    @Transactional(readOnly = true)
    public FileDownload downloadFile(Long submissionId, User principal) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission bulunamadı: " + submissionId));
        assertCanReadSubmission(principal, submission);

        if (submission.getSubmissionType() != SubmissionType.FILE_UPLOAD) {
            throw new IllegalArgumentException("Bu submission dosya tipinde değil.");
        }

        List<DeliverableSubmissionFile> files = submissionFileRepository.findBySubmission_IdOrderByIdDesc(submissionId);
        if (!files.isEmpty()) {
            DeliverableSubmissionFile f = files.get(0);
            return new FileDownload(resourceFromPath(Paths.get(f.getFilePath())), f.getOriginalFileName());
        }

        if (submission.getFilePath() != null) {
            String name = submission.getOriginalFileName() != null ? submission.getOriginalFileName() : "download";
            return new FileDownload(resourceFromPath(Paths.get(submission.getFilePath())), name);
        }

        throw new IllegalArgumentException("Dosya yolu bulunamadı.");
    }

    @Transactional(readOnly = true)
    public FileDownload downloadFileByFileId(Long fileId, User principal) {
        DeliverableSubmissionFile file = submissionFileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("Dosya bulunamadı: " + fileId));
        DeliverableSubmission submission = file.getSubmission();
        assertCanReadSubmission(principal, submission);
        return new FileDownload(resourceFromPath(Paths.get(file.getFilePath())), file.getOriginalFileName());
    }

    @Transactional
    public void deleteSubmissionFile(Long fileId, Long userId) {
        DeliverableSubmissionFile file = submissionFileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("Dosya bulunamadı: " + fileId));
        DeliverableSubmission submission = file.getSubmission();
        Long groupId = submission.getGroupId();

        UserGroupMember membership = userGroupMemberRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, GroupInviteStatus.ACCEPTED)
                .orElseThrow(() -> new IllegalArgumentException("Bu grubun üyesi değilsiniz."));
        if (membership.getRole() != GroupMembershipRole.LEADER) {
            throw new IllegalArgumentException("Sadece Team Leader dosya silebilir.");
        }

        if (file.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(file.getFilePath()));
            } catch (IOException e) {
                log.warn("Dosya disk'ten silinemedi: {}", e.getMessage());
            }
        }
        submissionFileRepository.delete(file);

        List<DeliverableSubmissionFile> remaining = submissionFileRepository.findBySubmission_IdOrderByIdDesc(submission.getId());
        if (remaining.isEmpty() && submission.getFilePath() == null && submission.getSubmissionType() == SubmissionType.FILE_UPLOAD) {
            submissionRepository.delete(submission);
            log.info("Submission silindi (son dosya kaldırıldı): id={}", submission.getId());
        }
    }

    @Transactional
    public void deleteSubmission(Long submissionId, Long userId) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission bulunamadı: " + submissionId));

        Long groupId = submission.getGroupId();

        UserGroupMember membership = userGroupMemberRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, GroupInviteStatus.ACCEPTED)
                .orElseThrow(() -> new IllegalArgumentException("Bu grubun üyesi değilsiniz."));

        if (membership.getRole() != GroupMembershipRole.LEADER) {
            throw new IllegalArgumentException("Sadece Team Leader dosya silebilir.");
        }

        removeAllStoredFilesFromDisk(submission);

        if (submission.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(submission.getFilePath()));
            } catch (IOException e) {
                log.warn("Legacy dosya disk'ten silinemedi: {}", e.getMessage());
            }
        }

        submissionRepository.delete(submission);
        log.info("Submission silindi: id={}, groupId={}, userId={}", submissionId, groupId, userId);
    }

    public Map<String, Object> toDto(DeliverableSubmission s) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", s.getId());
        dto.put("deliverableId", s.getDeliverable().getId());
        dto.put("groupId", s.getGroupId());
        dto.put("submittedByUserId", s.getSubmittedByUserId());
        dto.put("submissionType", s.getSubmissionType().name());
        dto.put("status", s.getStatus().name());
        dto.put("submittedAt", s.getSubmittedAt());
        dto.put("updatedAt", s.getUpdatedAt());

        if (s.getSubmissionType() == SubmissionType.FILE_UPLOAD) {
            dto.put("files", getSubmissionFilesDto(s.getId()));
            if (s.getOriginalFileName() != null) {
                dto.put("originalFileName", s.getOriginalFileName());
                dto.put("fileSize", s.getFileSize());
            }
        } else {
            dto.put("textContent", s.getTextContent());
        }
        return dto;
    }

    public List<Map<String, Object>> toDtoList(List<DeliverableSubmission> submissions) {
        return submissions.stream().map(this::toDto).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getSubmissionFilesDto(Long submissionId) {
        return submissionFileRepository.findBySubmission_IdOrderByIdDesc(submissionId).stream()
                .map(this::toFileDto)
                .toList();
    }

    private Map<String, Object> toFileDto(DeliverableSubmissionFile f) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", f.getId());
        dto.put("originalFileName", f.getOriginalFileName());
        dto.put("fileSize", f.getFileSize());
        dto.put("uploadedAt", f.getUploadedAt());
        return dto;
    }

    /** Bitmiş sprint için yeni teslim (dosya/metin) kabul edilmez. Bitiş günü dahil aynı gün hâlâ açık sayılır. */
    private void assertSprintAcceptsSubmission(ProjectDeliverable deliverable) {
        ProjectSprint sprint = deliverable.getSprint();
        if (sprint == null) {
            return;
        }
        LocalDate end = sprint.getEndDate();
        if (end != null && LocalDate.now().isAfter(end)) {
            throw new IllegalStateException("Bu sprintin teslim süresi sona ermiştir. Yeni yükleme yapılamaz.");
        }
    }

    private void assertCanReadSubmission(User principal, DeliverableSubmission submission) {
        if (principal == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        Role role = principal.getRole();
        if (role == Role.STUDENT) {
            Long groupId = submission.getGroupId();
            boolean member = userGroupMemberRepository
                    .findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), GroupInviteStatus.ACCEPTED)
                    .isPresent();
            if (!member) {
                throw new IllegalArgumentException("Bu submission'a erişiminiz yok.");
            }
        }
    }

    private static void removeAllStoredFilesFromDisk(DeliverableSubmission submission) {
        for (DeliverableSubmissionFile f : new ArrayList<>(submission.getFiles())) {
            if (f.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(f.getFilePath()));
                } catch (IOException e) {
                    log.warn("Dosya disk'ten silinemedi: {}", e.getMessage());
                }
            }
        }
    }

    private static Resource resourceFromPath(Path filePath) {
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("Dosya okunamadı: " + filePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Dosya URL hatası: " + e.getMessage());
        }
    }

    /** Strip path separators from client-provided name so storage stays under the intended directory. */
    private static String sanitizeStoredOriginalName(String name) {
        return name.replace('\\', '_').replace('/', '_');
    }
}
