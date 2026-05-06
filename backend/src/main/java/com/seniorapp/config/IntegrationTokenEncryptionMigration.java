package com.seniorapp.config;

import com.seniorapp.entity.UserGroup;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.service.IntegrationCredentialCryptoService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class IntegrationTokenEncryptionMigration {
    private final UserGroupRepository userGroupRepository;
    private final IntegrationCredentialCryptoService cryptoService;

    public IntegrationTokenEncryptionMigration(UserGroupRepository userGroupRepository,
                                               IntegrationCredentialCryptoService cryptoService) {
        this.userGroupRepository = userGroupRepository;
        this.cryptoService = cryptoService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateLegacyPlaintextTokens() {
        List<UserGroup> groups = userGroupRepository.findAll();
        for (UserGroup group : groups) {
            boolean changed = false;

            String github = group.getGithubPatEncrypted();
            if (github != null && !github.isBlank() && !cryptoService.isEncrypted(github)) {
                group.setGithubPatEncrypted(github.trim());
                changed = true;
            }

            String jira = group.getJiraSpaceUrlEncrypted();
            if (jira != null && !jira.isBlank() && !cryptoService.isEncrypted(jira)) {
                group.setJiraSpaceUrlEncrypted(jira.trim());
                changed = true;
            }

            if (changed) {
                // Entity listener encrypts values during save.
                userGroupRepository.save(group);
            }
        }
    }
}
