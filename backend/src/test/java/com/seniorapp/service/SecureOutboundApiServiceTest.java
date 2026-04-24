package com.seniorapp.service;

import com.seniorapp.exception.SecureDecryptionException;
import com.seniorapp.util.SecureLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Process 5.1 security features in SecureOutboundApiService
 */
@ExtendWith(MockitoExtension.class)
class SecureOutboundApiServiceTest {

    @Mock
    private IntegrationCredentialCryptoService cryptoService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SecureOutboundApiService outboundApiService;

    private final String VALID_ENCRYPTED_TOKEN = "ValidBase64EncodedToken";
    private final String VALID_PLAIN_TOKEN = "ghp_valid_token_123";
    private final String API_ENDPOINT = "https://api.github.com/user";
    private final String JIRA_ENDPOINT = "https://test.atlassian.net/rest/api/2/issue";

    @BeforeEach
    void setUp() {
        // Mock successful decryption by default
        when(cryptoService.decrypt(VALID_ENCRYPTED_TOKEN)).thenReturn(VALID_PLAIN_TOKEN);
        
        // Mock successful API response by default
        ResponseEntity<String> successResponse = ResponseEntity.ok("{\"success\": true}");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(successResponse);
    }

    @Test
    void testGitHubApiCall_Success() {
        // Act
        ResponseEntity<String> response = outboundApiService.executeGitHubApiCall(
                VALID_ENCRYPTED_TOKEN, API_ENDPOINT, null, HttpMethod.GET);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cryptoService).decrypt(VALID_ENCRYPTED_TOKEN);
        verify(restTemplate).exchange(eq(API_ENDPOINT), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testJiraApiCall_Success() {
        // Act
        ResponseEntity<String> response = outboundApiService.executeJiraApiCall(
                VALID_ENCRYPTED_TOKEN, JIRA_ENDPOINT, "{\"data\":\"test\"}", HttpMethod.POST);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cryptoService).decrypt(VALID_ENCRYPTED_TOKEN);
        verify(restTemplate).exchange(eq(JIRA_ENDPOINT), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testGitHubApiCall_DecryptionFailure_Returns500() {
        // Arrange
        when(cryptoService.decrypt(VALID_ENCRYPTED_TOKEN))
                .thenThrow(new IllegalStateException("Could not decrypt integration credentials"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            outboundApiService.executeGitHubApiCall(VALID_ENCRYPTED_TOKEN, API_ENDPOINT, null, HttpMethod.GET);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("GitHub authentication failed", exception.getReason());
    }

    @Test
    void testJiraApiCall_DecryptionFailure_Returns500() {
        // Arrange
        when(cryptoService.decrypt(VALID_ENCRYPTED_TOKEN))
                .thenThrow(new IllegalStateException("Could not decrypt integration credentials"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            outboundApiService.executeJiraApiCall(VALID_ENCRYPTED_TOKEN, JIRA_ENDPOINT, null, HttpMethod.GET);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Jira authentication failed", exception.getReason());
    }

    @Test
    void testGitHubApiCall_EmptyDecryptedToken_Returns500() {
        // Arrange
        when(cryptoService.decrypt(VALID_ENCRYPTED_TOKEN)).thenReturn("");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            outboundApiService.executeGitHubApiCall(VALID_ENCRYPTED_TOKEN, API_ENDPOINT, null, HttpMethod.GET);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("GitHub authentication failed", exception.getReason());
    }

    @Test
    void testJiraApiCall_NullDecryptedToken_Returns500() {
        // Arrange
        when(cryptoService.decrypt(VALID_ENCRYPTED_TOKEN)).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            outboundApiService.executeJiraApiCall(VALID_ENCRYPTED_TOKEN, JIRA_ENDPOINT, null, HttpMethod.GET);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Jira authentication failed", exception.getReason());
    }

    @Test
    void testGitHubApiCall_RestTemplateFailure_Returns500() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            outboundApiService.executeGitHubApiCall(VALID_ENCRYPTED_TOKEN, API_ENDPOINT, null, HttpMethod.GET);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("GitHub API call failed", exception.getReason());
    }

    @Test
    void testValidateEncryptedTokenFormat_ValidToken() {
        // Act & Assert
        assertTrue(outboundApiService.validateEncryptedTokenFormat(VALID_ENCRYPTED_TOKEN, "GitHub"));
    }

    @Test
    void testValidateEncryptedTokenFormat_NullToken() {
        // Act & Assert
        assertFalse(outboundApiService.validateEncryptedTokenFormat(null, "GitHub"));
    }

    @Test
    void testValidateEncryptedTokenFormat_EmptyToken() {
        // Act & Assert
        assertFalse(outboundApiService.validateEncryptedTokenFormat("", "GitHub"));
    }

    @Test
    void testValidateEncryptedTokenFormat_InvalidBase64() {
        // Act & Assert
        assertFalse(outboundApiService.validateEncryptedTokenFormat("InvalidBase64!!!", "GitHub"));
    }

    @Test
    void testGitHubApiCall_InvalidTokenFormat_Returns400() {
        // Arrange
        String invalidToken = "InvalidBase64!!!";

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            outboundApiService.executeGitHubApiCall(invalidToken, API_ENDPOINT, null, HttpMethod.GET);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid GitHub token format", exception.getReason());
        
        // Verify decryption was never attempted
        verify(cryptoService, never()).decrypt(anyString());
    }

    @Test
    void testJiraApiCall_InvalidTokenFormat_Returns400() {
        // Arrange
        String invalidToken = "";

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            outboundApiService.executeJiraApiCall(invalidToken, JIRA_ENDPOINT, null, HttpMethod.GET);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid Jira token format", exception.getReason());
        
        // Verify decryption was never attempted
        verify(cryptoService, never()).decrypt(anyString());
    }

    @Test
    void testSecureDecryptionException_Properties() {
        // Arrange
        String serviceType = "GitHub";
        Exception cause = new RuntimeException("Test cause");

        // Act
        SecureDecryptionException exception = new SecureDecryptionException(serviceType, cause);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals(serviceType + " authentication failed", exception.getMessage());
        assertEquals(serviceType, exception.getServiceType());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testSecureDecryptionException_NoCause() {
        // Arrange
        String serviceType = "Jira";

        // Act
        SecureDecryptionException exception = new SecureDecryptionException(serviceType);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals(serviceType + " authentication failed", exception.getMessage());
        assertEquals(serviceType, exception.getServiceType());
        assertNull(exception.getCause());
    }
}
