package com.seniorapp.service;

import com.seniorapp.dto.StudentIdValidityResponse;
import com.seniorapp.dto.ValidStudentListItemResponse;
import com.seniorapp.entity.ValidStudentId;
import com.seniorapp.repository.ValidStudentIdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for managing the pre-approved student whitelist
 * stored in the {@code valid_student_ids} table.
 *
 * <p>Only Coordinators and Admins are allowed to call the mutation
 * methods; that enforcement is done at the controller layer via
 * {@code @PreAuthorize}.</p>
 */
@Service
public class ValidStudentService {

    private static final Logger log = LoggerFactory.getLogger(ValidStudentService.class);
    private static final DateTimeFormatter ADDED_DATE_JSON = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ValidStudentIdRepository validStudentIdRepository;

    public ValidStudentService(ValidStudentIdRepository validStudentIdRepository) {
        this.validStudentIdRepository = validStudentIdRepository;
    }

    // -------------------------------------------------------
    // Write operations
    // -------------------------------------------------------

    /**
     * Adds a list of student identifiers (e-mails or ID strings) to the
     * whitelist. Entries that are already present are silently skipped to
     * keep the operation idempotent.
     *
     * @param studentIds  list of student e-mails / IDs to whitelist
     * @param addedBy     username / email of the coordinator performing the upload
     * @return number of new entries actually persisted
     */
    @Transactional
    public int uploadStudentIds(List<String> studentIds, String addedBy) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0;
        }
        Set<String> seenInRequest = new LinkedHashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String raw : studentIds) {
            if (raw == null) {
                continue;
            }
            String id = raw.trim();
            if (id.isEmpty() || !seenInRequest.add(id)) {
                continue;
            }
            normalized.add(id);
        }

        List<ValidStudentId> newEntries = normalized.stream()
                .filter(id -> !validStudentIdRepository.existsByStudentId(id))
                .map(id -> {
                    ValidStudentId entry = new ValidStudentId();
                    entry.setStudentId(id);
                    entry.setAddedBy(addedBy != null ? addedBy : "system");
                    return entry;
                })
                .toList();

        validStudentIdRepository.saveAll(newEntries);
        int count = newEntries.size();
        log.info("Whitelist persisted: {} new unique row(s), {} candidate id(s) in request, addedBy={}",
                count, normalized.size(), addedBy != null ? addedBy : "system");
        return count;
    }

    /**
     * Removes a whitelist row only if no user account is linked yet.
     */
    @Transactional
    public void deleteWhitelistEntry(Long entryId) {
        ValidStudentId row = validStudentIdRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Whitelist entry not found."));
        if (row.getAccount() != null) {
            throw new RuntimeException("Cannot remove: this student ID is already linked to an account.");
        }
        validStudentIdRepository.delete(row);
        log.info("Whitelist entry removed id={} studentId={}", entryId, row.getStudentId());
    }

    // -------------------------------------------------------
    // Read operations
    // -------------------------------------------------------

    /**
     * Checks whether a given student identifier exists in the whitelist.
     *
     * @param studentId the e-mail or ID string to check
     * @return {@code true} if the identifier is on the whitelist
     */
    public boolean isWhitelisted(String studentId) {
        return validStudentIdRepository.existsByStudentId(studentId);
    }

    /**
     * Public student-login step: validates ID and reports whether a user account is linked.
     */
    @Transactional(readOnly = true)
    public StudentIdValidityResponse checkStudentIdValidity(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return new StudentIdValidityResponse("", false, false, null);
        }
        String sid = studentId.trim();
        return validStudentIdRepository.findByStudentId(sid)
                .map(v -> {
                    boolean linked = v.getAccount() != null;
                    Long accountId = linked ? v.getAccount().getId() : null;
                    return new StudentIdValidityResponse(sid, true, linked, accountId);
                })
                .orElseGet(() -> new StudentIdValidityResponse(sid, false, false, null));
    }

    /**
     * Returns all whitelist entries that have not yet been linked to
     * a {@link com.seniorapp.entity.User} account.
     *
     * @return list of pending (unregistered) whitelist entries
     */
    public List<ValidStudentId> getPendingEntries() {
        return validStudentIdRepository.findByAccountIsNull();
    }

    /**
     * Returns all whitelist entries that have been linked to an
     * existing {@link com.seniorapp.entity.User} account.
     *
     * @return list of registered whitelist entries
     */
    public List<ValidStudentId> getRegisteredEntries() {
        return validStudentIdRepository.findByAccountIsNotNull();
    }

    /**
     * All whitelist rows for coordinator UI, newest first.
     */
    @Transactional(readOnly = true)
    public List<ValidStudentListItemResponse> listAllWhitelistEntries() {
        return validStudentIdRepository.findAllWhitelistWithAccountOrderByAddedDateDesc().stream()
                .map(v -> {
                    boolean linked = v.getAccount() != null;
                    Long accId = linked ? v.getAccount().getId() : null;
                    String added = v.getAddedDate() != null ? ADDED_DATE_JSON.format(v.getAddedDate()) : null;
                    return new ValidStudentListItemResponse(
                            v.getId(),
                            v.getStudentId(),
                            linked,
                            accId,
                            added,
                            v.getAddedBy()
                    );
                })
                .toList();
    }
}
