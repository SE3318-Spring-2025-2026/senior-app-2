package com.seniorapp.service;

import com.seniorapp.entity.ValidStudentId;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.repository.ValidStudentIdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private final ValidStudentIdRepository validStudentIdRepository;
    private final UserRepository userRepository;

    public ValidStudentService(ValidStudentIdRepository validStudentIdRepository,
                               UserRepository userRepository) {
        this.validStudentIdRepository = validStudentIdRepository;
        this.userRepository = userRepository;
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
        List<ValidStudentId> newEntries = studentIds.stream()
                .filter(id -> !validStudentIdRepository.existsByStudentId(id))
                .map(id -> {
                    ValidStudentId entry = new ValidStudentId();
                    entry.setStudentId(id);
                    entry.setAddedBy(addedBy);
                    return entry;
                })
                .toList();

        validStudentIdRepository.saveAll(newEntries);
        return newEntries.size();
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
}
