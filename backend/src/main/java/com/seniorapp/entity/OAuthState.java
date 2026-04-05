package com.seniorapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_states")
public class OAuthState {

    /**
     * Benzersiz ve tahmin edilemez state değeri.
     */
    @Id
    private String state;

    /**
     * State'in oluşturulma zamanı. Süresi dolan state'leri temizlemek için kullanılabilir.
     */
    private LocalDateTime createdAt;

    /**
     * Öğrenci GitHub akışında: doğrulanmış öğrenci kimliği (LINK / LOGIN).
     */
    private String contextStudentId;

    /**
     * {@code LINK} = ilk kez GitHub ile hesap oluştur ve whitelist satırına bağla;
     * {@code LOGIN} = mevcut bağlı hesapla giriş.
     */
    private String oauthFlow;

    public OAuthState() {}

    public OAuthState(String state, LocalDateTime createdAt) {
        this.state = state;
        this.createdAt = createdAt;
    }

    public OAuthState(String state, LocalDateTime createdAt, String contextStudentId, String oauthFlow) {
        this.state = state;
        this.createdAt = createdAt;
        this.contextStudentId = contextStudentId;
        this.oauthFlow = oauthFlow;
    }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getContextStudentId() { return contextStudentId; }
    public void setContextStudentId(String contextStudentId) { this.contextStudentId = contextStudentId; }

    public String getOauthFlow() { return oauthFlow; }
    public void setOauthFlow(String oauthFlow) { this.oauthFlow = oauthFlow; }
}