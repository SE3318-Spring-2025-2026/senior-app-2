package com.seniorapp.service;

import com.seniorapp.entity.*;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deliverable submission işlemlerini yöneten servis.
 * Dosya yükleme ve metin editörü ile gönderim destekler.
 * Dosyalar disk üzerinde storage/{templateId}/{groupId}/{deliverableId}/ altında saklanır.
 */
@Service
public class DeliverableSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(DeliverableSubmissionService.class);

    /** Dangerous file extensions that should be blocked */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".com", ".msi", ".scr", ".pif",
            ".vbs", ".vbe", ".js", ".jse", ".wsh", ".wsf",
            ".ps1", ".psm1", ".psd1", ".reg", ".inf", ".hta"
    );

    private final DeliverableSubmissionRepository submissionRepository;
    private final ProjectDeliverableRepository deliverableRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;

    @Value("${app.storage.base-path:./storage}")
    private String storageBasePath;

    public DeliverableSubmissionService(
            DeliverableSubmissionRepository submissionRepository,
            ProjectDeliverableRepository deliverableRepository,
            UserGroupMemberRepository userGroupMemberRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.deliverableRepository = deliverableRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
    }

    /**
     * Dosya yükleme ile submission oluşturur veya günceller.
     * Dosya storage/{templateId}/{groupId}/{deliverableId}/ altına kaydedilir.
     * Eğer mevcut bir submission varsa, eski dosya disk'ten silinir.
     */
    @Transactional
    public DeliverableSubmission submitFile(Long deliverableId, Long groupId, Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        ProjectDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new NoSuchElementException("Deliverable not found: " + deliverableId));

        // Template ID'yi bul (Project → Template)
        Long templateId = deliverable.getSprint().getProject().getTemplate().getId();

        // Dosya adını sanitize et
        String originalFileName = sanitizeFileName(file.getOriginalFilename());

        // Dosya uzantısı kontrolü
        validateFileExtension(originalFileName);

        Path storagePath = Paths.get(storageBasePath,
                String.valueOf(templateId),
                String.valueOf(groupId),
                String.valueOf(deliverableId));

        try {
            Files.createDirectories(storagePath);

            // Mevcut submission varsa eski dosyayı sil
            Optional<DeliverableSubmission> existingSubmission =
                    submissionRepository.findByDeliverableIdAndGroupId(deliverableId, groupId);
            if (existingSubmission.isPresent() && existingSubmission.get().getFilePath() != null) {
                deleteFileFromDisk(existingSubmission.get().getFilePath());
            }

            Path targetFile = storagePath.resolve(originalFileName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("File saved: {} (size: {} bytes)", targetFile, file.getSize());

            // Submission oluştur veya güncelle
            DeliverableSubmission submission = existingSubmission.orElse(new DeliverableSubmission());

            submission.setDeliverable(deliverable);
            submission.setGroupId(groupId);
            submission.setSubmittedByUserId(userId);
            submission.setSubmissionType(SubmissionType.FILE_UPLOAD);
            submission.setFilePath(targetFile.toString());
            submission.setOriginalFileName(originalFileName);
            submission.setFileSize(file.getSize());
            submission.setTextContent(null);
            submission.setStatus(SubmissionStatus.SUBMITTED);

            return submissionRepository.save(submission);

        } catch (IOException e) {
            log.error("File save error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
    }

    /**
     * Metin editörü ile submission oluşturur veya günceller.
     */
    @Transactional
    public DeliverableSubmission submitText(Long deliverableId, Long groupId, Long userId, String textContent) {
        if (textContent == null || textContent.isBlank()) {
            throw new IllegalArgumentException("Text content cannot be empty.");
        }

        ProjectDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new NoSuchElementException("Deliverable not found: " + deliverableId));

        DeliverableSubmission submission = submissionRepository
                .findByDeliverableIdAndGroupId(deliverableId, groupId)
                .orElse(new DeliverableSubmission());

        // Eğer mevcut submission dosya tipindeyse, eski dosyayı sil
        if (submission.getSubmissionType() == SubmissionType.FILE_UPLOAD && submission.getFilePath() != null) {
            deleteFileFromDisk(submission.getFilePath());
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

    /**
     * Belirli bir deliverable ve grup için olan submission'ı getirir.
     */
    @Transactional(readOnly = true)
    public Optional<DeliverableSubmission> getSubmission(Long deliverableId, Long groupId) {
        return submissionRepository.findByDeliverableIdAndGroupId(deliverableId, groupId);
    }

    /**
     * Bir projeye ait belirli grubun tüm submission'larını getirir.
     */
    @Transactional(readOnly = true)
    public List<DeliverableSubmission> getSubmissionsByProject(Long projectId, Long groupId) {
        return submissionRepository.findByDeliverable_Sprint_Project_IdAndGroupId(projectId, groupId);
    }

    /**
     * ID ile submission getirir (download endpoint için).
     */
    @Transactional(readOnly = true)
    public Optional<DeliverableSubmission> getSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId);
    }

    /**
     * Submission'a ait dosyayı indirmek için Resource döner.
     */
    @Transactional(readOnly = true)
    public Resource downloadFile(Long submissionId) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found: " + submissionId));

        if (submission.getSubmissionType() != SubmissionType.FILE_UPLOAD) {
            throw new IllegalArgumentException("This submission is not a file upload type.");
        }

        if (submission.getFilePath() == null) {
            throw new IllegalArgumentException("File path not found for this submission.");
        }

        try {
            Path filePath = Paths.get(submission.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not readable: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File URL error: " + e.getMessage());
        }
    }

    /**
     * Submission'ı siler. Sadece grubun Team Leader'ı silebilir.
     * Dosya tipindeyse disk'ten de silinir.
     */
    @Transactional
    public void deleteSubmission(Long submissionId, Long userId) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found: " + submissionId));

        Long groupId = submission.getGroupId();

        // Kullanıcının bu grupta LEADER olup olmadığını kontrol et
        UserGroupMember membership = userGroupMemberRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, GroupInviteStatus.ACCEPTED)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group."));

        if (membership.getRole() != GroupMembershipRole.LEADER) {
            throw new IllegalArgumentException("Only the Team Leader can delete submissions.");
        }

        // Dosya tipindeyse disk'ten sil
        if (submission.getSubmissionType() == SubmissionType.FILE_UPLOAD && submission.getFilePath() != null) {
            deleteFileFromDisk(submission.getFilePath());
        }

        submissionRepository.delete(submission);
        log.info("Submission deleted: id={}, groupId={}, userId={}", submissionId, groupId, userId);
    }

    /**
     * Submission'ı DTO map'e dönüştürür.
     */
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
            dto.put("originalFileName", s.getOriginalFileName());
            dto.put("fileSize", s.getFileSize());
        } else {
            dto.put("textContent", s.getTextContent());
        }
        return dto;
    }

    /**
     * Submission listesini DTO listesine dönüştürür.
     */
    public List<Map<String, Object>> toDtoList(List<DeliverableSubmission> submissions) {
        return submissions.stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Private Helpers ──

    /**
     * Sanitizes file name to prevent path traversal and XSS.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed_file";
        }
        // Remove path components
        String sanitized = Paths.get(fileName).getFileName().toString();
        // Remove control characters and special chars
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");
        // Ensure it's not empty after sanitization
        if (sanitized.isBlank()) {
            sanitized = "unnamed_file";
        }
        return sanitized;
    }

    /**
     * Validates file extension against blocked list.
     */
    private void validateFileExtension(String fileName) {
        if (fileName == null) return;
        String lower = fileName.toLowerCase();
        for (String ext : BLOCKED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                throw new IllegalArgumentException(
                        "File type '" + ext + "' is not allowed. Please upload a safe file type.");
            }
        }
    }

    /**
     * Safely deletes a file from disk.
     */
    private void deleteFileFromDisk(String filePathStr) {
        try {
            Path filePath = Paths.get(filePathStr);
            if (Files.deleteIfExists(filePath)) {
                log.info("File deleted from disk: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file from disk: {}", e.getMessage());
        }
    }
}
