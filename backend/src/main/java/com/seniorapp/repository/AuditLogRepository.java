package com.seniorapp.repository;

import com.seniorapp.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Tüm loglar — oluşturulma tarihine göre azalan */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Modüle göre filtrele */
    Page<AuditLog> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);

    /** Severity'ye göre filtrele */
    Page<AuditLog> findBySeverityOrderByCreatedAtDesc(String severity, Pageable pageable);

    /** Belirli bir kullanıcının logları */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Sadece güvenlik olayları */
    Page<AuditLog> findByIsSecurityEventTrueOrderByCreatedAtDesc(Pageable pageable);

    /** Sadece kritik olaylar */
    Page<AuditLog> findByIsCriticalEventTrueOrderByCreatedAtDesc(Pageable pageable);

    /** Modül + status kombinasyonu */
    Page<AuditLog> findByModuleAndStatusOrderByCreatedAtDesc(String module, String status, Pageable pageable);

    /** Modül + severity kombinasyonu */
    Page<AuditLog> findByModuleAndSeverityOrderByCreatedAtDesc(String module, String severity, Pageable pageable);

    /** Tarih aralığı */
    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** Belirli sayıda günden eski logları sil */
    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** Toplam log sayısı (modüle göre isteğe bağlı) */
    long countByModule(String module);

    /** Toplam güvenlik olayı sayısı */
    long countByIsSecurityEventTrue();

    /** Belirli severity'deki toplam sayı */
    long countBySeverity(String severity);
}
