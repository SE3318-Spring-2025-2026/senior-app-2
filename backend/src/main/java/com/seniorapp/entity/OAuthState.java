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

    public OAuthState() {}

    public OAuthState(String state, LocalDateTime createdAt) {
        this.state = state;
        this.createdAt = createdAt;
    }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}