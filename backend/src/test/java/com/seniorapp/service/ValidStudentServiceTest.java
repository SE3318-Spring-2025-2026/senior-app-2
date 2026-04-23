package com.seniorapp.service;

import com.seniorapp.dto.StudentIdValidityResponse;
import com.seniorapp.dto.ValidStudentListItemResponse;
import com.seniorapp.entity.User;
import com.seniorapp.entity.ValidStudentId;
import com.seniorapp.repository.ValidStudentIdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidStudentService Tests")
public class ValidStudentServiceTest {

    @Mock
    private ValidStudentIdRepository validStudentIdRepository;

    @InjectMocks
    private ValidStudentService validStudentService;

    @BeforeEach
    void setUp() {
        // setUp if needed
    }

    @Test
    @DisplayName("Upload student IDs - Successful new entries")
    void testUploadStudentIds_Success() {
        List<String> inputIds = Arrays.asList("student1@test.com", "student2@test.com", "student1@test.com", "");
        
        when(validStudentIdRepository.existsByStudentId("student1@test.com")).thenReturn(false);
        when(validStudentIdRepository.existsByStudentId("student2@test.com")).thenReturn(true);
        when(validStudentIdRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int result = validStudentService.uploadStudentIds(inputIds, "admin");

        assertEquals(1, result);
        verify(validStudentIdRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Upload student IDs - Null or empty list")
    void testUploadStudentIds_NullOrEmpty() {
        assertEquals(0, validStudentService.uploadStudentIds(null, "admin"));
        assertEquals(0, validStudentService.uploadStudentIds(Collections.emptyList(), "admin"));
        verify(validStudentIdRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Delete whitelist entry - Success")
    void testDeleteWhitelistEntry_Success() {
        ValidStudentId entry = new ValidStudentId();
        entry.setId(1L);
        entry.setStudentId("student@test.com");
        
        when(validStudentIdRepository.findById(1L)).thenReturn(Optional.of(entry));
        
        assertDoesNotThrow(() -> validStudentService.deleteWhitelistEntry(1L));
        verify(validStudentIdRepository, times(1)).delete(entry);
    }

    @Test
    @DisplayName("Delete whitelist entry - Fails when linked to account")
    void testDeleteWhitelistEntry_LinkedAccount() {
        ValidStudentId entry = new ValidStudentId();
        entry.setId(1L);
        entry.setStudentId("student@test.com");
        entry.setAccount(new User()); // Linked to account
        
        when(validStudentIdRepository.findById(1L)).thenReturn(Optional.of(entry));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> validStudentService.deleteWhitelistEntry(1L));
        assertEquals("Cannot remove: this student ID is already linked to an account.", exception.getMessage());
        verify(validStudentIdRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Check student ID validity - Not found")
    void testCheckStudentIdValidity_NotFound() {
        when(validStudentIdRepository.findByStudentId("unknown@test.com")).thenReturn(Optional.empty());
        
        StudentIdValidityResponse response = validStudentService.checkStudentIdValidity("unknown@test.com");
        
        assertEquals("unknown@test.com", response.studentId());
        assertFalse(response.valid());
        assertFalse(response.linked());
        assertNull(response.matchedAccountId());
    }

    @Test
    @DisplayName("Check student ID validity - Found and not linked")
    void testCheckStudentIdValidity_FoundNotLinked() {
        ValidStudentId entry = new ValidStudentId();
        entry.setStudentId("valid@test.com");
        
        when(validStudentIdRepository.findByStudentId("valid@test.com")).thenReturn(Optional.of(entry));
        
        StudentIdValidityResponse response = validStudentService.checkStudentIdValidity("valid@test.com");
        
        assertEquals("valid@test.com", response.studentId());
        assertTrue(response.valid());
        assertFalse(response.linked());
        assertNull(response.matchedAccountId());
    }
    
    @Test
    @DisplayName("Check student ID validity - Found and linked")
    void testCheckStudentIdValidity_FoundAndLinked() {
        ValidStudentId entry = new ValidStudentId();
        entry.setStudentId("linked@test.com");
        User account = new User();
        account.setId(99L);
        entry.setAccount(account);
        
        when(validStudentIdRepository.findByStudentId("linked@test.com")).thenReturn(Optional.of(entry));
        
        StudentIdValidityResponse response = validStudentService.checkStudentIdValidity("linked@test.com");
        
        assertEquals("linked@test.com", response.studentId());
        assertTrue(response.valid());
        assertTrue(response.linked());
        assertEquals(99L, response.matchedAccountId());
    }

    @Test
    @DisplayName("List all whitelist entries")
    void testListAllWhitelistEntries() {
        ValidStudentId entry = new ValidStudentId();
        entry.setId(1L);
        entry.setStudentId("test@test.com");
        entry.setAddedBy("admin");
        entry.setAddedDate(LocalDateTime.now());
        
        when(validStudentIdRepository.findAllWhitelistWithAccountOrderByAddedDateDesc())
                .thenReturn(Collections.singletonList(entry));
                
        List<ValidStudentListItemResponse> list = validStudentService.listAllWhitelistEntries();
        
        assertEquals(1, list.size());
        assertEquals("test@test.com", list.get(0).getStudentId());
        assertEquals("admin", list.get(0).getAddedBy());
        assertFalse(list.get(0).isLinked());
    }
}
