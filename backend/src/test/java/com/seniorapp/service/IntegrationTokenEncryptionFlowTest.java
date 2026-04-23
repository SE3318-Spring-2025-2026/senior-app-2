package com.seniorapp.service;

import com.seniorapp.config.IntegrationTokenEncryptionMigration;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupIntegrationTokenEncryptionListener;
import com.seniorapp.repository.UserGroupRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationTokenEncryptionFlowTest {

    private final IntegrationCredentialCryptoService cryptoService =
            new IntegrationCredentialCryptoService("integration_test_secret_key_1234567890", "");

    @Test
    void encryptFormatContainsIvAndCiphertextAndDecryptsBack() {
        String encrypted = cryptoService.encrypt("github_pat_example_token");

        assertTrue(encrypted.startsWith("enc:v1:"));
        assertEquals("github_pat_example_token", cryptoService.decrypt(encrypted));
    }

    @Test
    void listenerEncryptsBeforePersistOrUpdate() {
        UserGroupIntegrationTokenEncryptionListener listener = new UserGroupIntegrationTokenEncryptionListener();
        listener.setCryptoService(cryptoService);

        UserGroup group = new UserGroup();
        group.setGithubPatEncrypted("github_pat_plaintext_value");
        group.setJiraSpaceUrlEncrypted("https://jira.example.com");

        listener.encryptBeforeWrite(group);

        String encryptedGithub = group.getGithubPatEncrypted();
        String encryptedJira = group.getJiraSpaceUrlEncrypted();

        assertNotEquals("github_pat_plaintext_value", encryptedGithub);
        assertNotEquals("https://jira.example.com", encryptedJira);
        assertEquals("github_pat_plaintext_value", cryptoService.decrypt(encryptedGithub));
        assertEquals("https://jira.example.com", cryptoService.decrypt(encryptedJira));
    }

    @Test
    void migrationSavesOnlyLegacyPlaintextRecords() {
        UserGroupRepository repository = mock(UserGroupRepository.class);
        IntegrationTokenEncryptionMigration migration = new IntegrationTokenEncryptionMigration(repository, cryptoService);

        UserGroup plainGroup = new UserGroup();
        plainGroup.setGithubPatEncrypted("github_pat_plain");
        plainGroup.setJiraSpaceUrlEncrypted("https://jira.plain.example");

        UserGroup alreadyEncryptedGroup = new UserGroup();
        alreadyEncryptedGroup.setGithubPatEncrypted(cryptoService.encrypt("github_pat_encrypted"));
        alreadyEncryptedGroup.setJiraSpaceUrlEncrypted(cryptoService.encrypt("https://jira.encrypted.example"));

        when(repository.findAll()).thenReturn(List.of(plainGroup, alreadyEncryptedGroup));

        migration.migrateLegacyPlaintextTokens();

        verify(repository, times(1)).save(plainGroup);
    }
}
