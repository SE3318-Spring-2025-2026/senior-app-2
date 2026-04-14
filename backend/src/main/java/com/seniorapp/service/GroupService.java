package com.seniorapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

    public void createGroup(String studentId, String groupName, String projectId) {
        System.out.println("Grup Kurulum İsteği -> Lider: " + studentId + " | Grup: " + groupName + " | Proje: " + projectId);

        // Kriter 1: Değerler boş gelemez
        if (studentId == null || studentId.isBlank() ||
            groupName == null || groupName.isBlank() ||
            projectId == null || projectId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad Request: 'groupName', 'studentId', and 'projectId' fields are strictly required.");
        }

        // Kriter 4 (409 Conflict): Grup adı alınmış mı veya öğrenci zaten grupta mı?
        // Test için frontend'den grup adına "taken" veya öğrenci id'sine "std-taken" gönderirsen patlar.
        if ("taken".equalsIgnoreCase(groupName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conflict: Group name is already taken.");
        }
        if ("std-taken".equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conflict: Student is already a member of another group.");
        }

        // Kriter 2 & 3: Başarılı Senaryo (Veritabanı bağlanınca buraya repository.save eklenecek)
        System.out.println("Başarılı: '" + groupName + "' grubu kuruldu.");
        System.out.println("Başarılı: " + studentId + " 'Team Leader' olarak atandı.");
        System.out.println("Başarılı: '" + projectId + "' şablonundan Actual Project instance'ı oluşturuldu.");
    }
}