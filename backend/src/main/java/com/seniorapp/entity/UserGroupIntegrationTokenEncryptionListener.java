package com.seniorapp.entity;

import com.seniorapp.service.IntegrationCredentialCryptoService;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserGroupIntegrationTokenEncryptionListener {
    private static IntegrationCredentialCryptoService cryptoService;

    @Autowired
    public void setCryptoService(IntegrationCredentialCryptoService cryptoService) {
        UserGroupIntegrationTokenEncryptionListener.cryptoService = cryptoService;
    }

    @PrePersist
    @PreUpdate
    public void encryptBeforeWrite(UserGroup userGroup) {
        if (cryptoService == null) {
            throw new IllegalStateException("Integration credential crypto service is not initialized.");
        }

        userGroup.setGithubPatEncrypted(cryptoService.encryptIfNeeded(userGroup.getGithubPatEncrypted()));
        userGroup.setJiraSpaceUrlEncrypted(cryptoService.encryptIfNeeded(userGroup.getJiraSpaceUrlEncrypted()));
    }
}
