package de.dreistrom.document.repository;

import de.dreistrom.document.domain.Document;
import de.dreistrom.document.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByUploadedAtDesc(Long userId);

    List<Document> findByUserIdAndDocumentTypeOrderByUploadedAtDesc(Long userId, DocumentType type);

    Optional<Document> findByUserIdAndSha256Hash(Long userId, String sha256Hash);

    @Query("SELECT d FROM Document d WHERE d.retentionUntil <= :date AND d.deletionLocked = true")
    List<Document> findExpiredRetention(LocalDate date);

    @Query("SELECT d FROM Document d WHERE d.retentionUntil BETWEEN :from AND :to AND d.deletionLocked = true")
    List<Document> findApproachingExpiry(LocalDate from, LocalDate to);
}
