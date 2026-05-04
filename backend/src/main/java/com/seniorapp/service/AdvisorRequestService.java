package com.seniorapp.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdvisorRequestService {

    public void createRequest(String groupId, String professorId) {
        System.out.println("Gelen İstek -> Grup: " + groupId + " | Hoca: " + professorId);

        // 409 CONFLICT: Eğer grup zaten bir hoca seçtiyse
        // (Test için frontend'den professorId olarak "409" yollarsan bu çalışır)
        if ("409".equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your group is already in the assignment process.");
        }

        // 403 FORBIDDEN: Eğer hoca o komitede değilse
        // (Test için frontend'den professorId olarak "403" yollarsan bu çalışır)
        if ("403".equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This professor is not in the required committee.");
        }

        System.out.println("İşlem Başarılı: İstek oluşturuldu.");
    }

    public void processDecision(String requestId, String currentProfessorId, String decision) {
        if (!"approve".equals(decision) && !"decline".equals(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid decision type.");
        }
        System.out.println("Hoca Kararı: " + decision);
    }
}