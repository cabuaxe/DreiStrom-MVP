package de.dreistrom.document.domain;

import de.dreistrom.common.domain.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "document")
@Getter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "s3_key", nullable = false, length = 1000)
    private String s3Key;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "retention_years", nullable = false)
    private int retentionYears;

    @Column(name = "retention_until", nullable = false)
    private LocalDate retentionUntil;

    @Column(name = "deletion_locked", nullable = false)
    private boolean deletionLocked = true;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public Document(AppUser user, String fileName, String contentType,
                    long fileSize, String s3Key, String sha256Hash,
                    DocumentType documentType, String description) {
        this.user = user;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.s3Key = s3Key;
        this.sha256Hash = sha256Hash;
        this.documentType = documentType;
        this.description = description;
        this.retentionYears = documentType.retentionYears();
        this.retentionUntil = LocalDate.now().plusYears(retentionYears);
        this.deletionLocked = true;
        this.uploadedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Check if deletion is allowed per §147 AO retention rules.
     */
    public boolean canDelete() {
        return !deletionLocked || LocalDate.now().isAfter(retentionUntil);
    }

    /**
     * Unlock for deletion after retention period expires.
     */
    public void unlockDeletion() {
        if (LocalDate.now().isAfter(retentionUntil)) {
            this.deletionLocked = false;
            this.updatedAt = Instant.now();
        }
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void updateTags(String tags) {
        this.tags = tags;
        this.updatedAt = Instant.now();
    }
}
