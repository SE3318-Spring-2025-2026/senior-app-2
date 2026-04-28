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
     */
    @Transactional
    public DeliverableSubmission submitFile(Long deliverableId, Long groupId, Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz.");
        }

        ProjectDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new NoSuchElementException("Deliverable bulunamadı: " + deliverableId));

        // Template ID'yi bul (Project → Template)
        Long templateId = deliverable.getSprint().getProject().getTemplate().getId();

        // Dosyayı diske kaydet
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "unnamed_file";
        }

        Path storagePath = Paths.get(storageBasePath,
                String.valueOf(templateId),
                String.valueOf(groupId),
                String.valueOf(deliverableId));

        try {
            Files.createDirectories(storagePath);

            // Aynı isimde dosya varsa üzerine yaz
            Path targetFile = storagePath.resolve(originalFileName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("Dosya kaydedildi: {} (boyut: {} byte)", targetFile, file.getSize());

            // Submission oluştur veya güncelle
            DeliverableSubmission submission = submissionRepository
                    .findByDeliverableIdAndGroupId(deliverableId, groupId)
                    .orElse(new DeliverableSubmission());

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
            log.error("Dosya kaydetme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Dosya kaydedilemedi: " + e.getMessage());
        }
    }

    /**
     * Metin editörü ile submission oluşturur veya günceller.
     */
    @Transactional
    public DeliverableSubmission submitText(Long deliverableId, Long groupId, Long userId, String textContent) {
        if (textContent == null || textContent.isBlank()) {
            throw new IllegalArgumentException("Metin içeriği boş olamaz.");
        }

        ProjectDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new NoSuchElementException("Deliverable bulunamadı: " + deliverableId));

        DeliverableSubmission submission = submissionRepository
                .findByDeliverableIdAndGroupId(deliverableId, groupId)
                .orElse(new DeliverableSubmission());

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
                .orElseThrow(() -> new NoSuchElementException("Submission bulunamadı: " + submissionId));

        if (submission.getSubmissionType() != SubmissionType.FILE_UPLOAD) {
            throw new IllegalArgumentException("Bu submission dosya tipinde değil.");
        }

        if (submission.getFilePath() == null) {
            throw new IllegalArgumentException("Dosya yolu bulunamadı.");
        }

        try {
            Path filePath = Paths.get(submission.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Dosya okunamadı: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Dosya URL hatası: " + e.getMessage());
        }
    }

    /**
     * Submission'ı siler. Sadece grubun Team Leader'ı silebilir.
     * Dosya tipindeyse disk'ten de silinir.
     */
    @Transactional
    public void deleteSubmission(Long submissionId, Long userId) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission bulunamadı: " + submissionId));

        Long groupId = submission.getGroupId();

        // Kullanıcının bu grupta LEADER olup olmadığını kontrol et
        UserGroupMember membership = userGroupMemberRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, GroupInviteStatus.ACCEPTED)
                .orElseThrow(() -> new IllegalArgumentException("Bu grubun üyesi değilsiniz."));

        if (membership.getRole() != GroupMembershipRole.LEADER) {
            throw new IllegalArgumentException("Sadece Team Leader dosya silebilir.");
        }

        // Dosya tipindeyse disk'ten sil
        if (submission.getSubmissionType() == SubmissionType.FILE_UPLOAD && submission.getFilePath() != null) {
            try {
                Path filePath = Paths.get(submission.getFilePath());
                Files.deleteIfExists(filePath);
                log.info("Dosya disk'ten silindi: {}", filePath);
            } catch (IOException e) {
                log.warn("Dosya disk'ten silinemedi: {}", e.getMessage());
            }
        }

        submissionRepository.delete(submission);
        log.info("Submission silindi: id={}, groupId={}, userId={}", submissionId, groupId, userId);
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
}
