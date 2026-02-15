package de.dreistrom.document.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.document.domain.Document;
import de.dreistrom.document.domain.DocumentType;
import de.dreistrom.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVaultService {

    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;

    /**
     * Upload a document: compute SHA-256, store in S3, create DB record.
     */
    @Transactional
    public Document upload(AppUser user, String fileName, String contentType,
                           byte[] content, DocumentType documentType, String description) {
        String sha256 = computeSha256(content);

        // Check for duplicate by hash
        documentRepository.findByUserIdAndSha256Hash(user.getId(), sha256)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Duplicate document: file with same SHA-256 hash already exists (id=%d)"
                                    .formatted(existing.getId()));
                });

        String s3Key = s3StorageService.upload(user.getId(), fileName, contentType, content);

        Document doc = new Document(user, fileName, contentType,
                content.length, s3Key, sha256, documentType, description);

        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public Document getById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Document> listByUser(Long userId) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Document> listByUserAndType(Long userId, DocumentType type) {
        return documentRepository.findByUserIdAndDocumentTypeOrderByUploadedAtDesc(userId, type);
    }

    /**
     * Get a pre-signed download URL for a document.
     */
    @Transactional(readOnly = true)
    public URL getDownloadUrl(Long documentId) {
        Document doc = getById(documentId);
        return s3StorageService.getPresignedDownloadUrl(doc.getS3Key());
    }

    /**
     * Delete a document if retention period has passed.
     * Blocks premature deletion per §147 AO.
     */
    @Transactional
    public void delete(Long documentId) {
        Document doc = getById(documentId);

        if (!doc.canDelete()) {
            throw new IllegalStateException(
                    "Cannot delete document %d: retention period until %s (§147 AO)"
                            .formatted(documentId, doc.getRetentionUntil()));
        }

        s3StorageService.delete(doc.getS3Key());
        documentRepository.delete(doc);
    }

    /**
     * Update document metadata (description, tags).
     */
    @Transactional
    public Document updateMetadata(Long documentId, String description, String tags) {
        Document doc = getById(documentId);
        if (description != null) doc.updateDescription(description);
        if (tags != null) doc.updateTags(tags);
        return doc;
    }

    /**
     * Nightly job: unlock documents whose retention has expired,
     * and flag documents approaching expiry (within 90 days).
     */
    @Transactional
    public void processRetentionExpiry() {
        LocalDate today = LocalDate.now();

        // Unlock expired retention
        List<Document> expired = documentRepository.findExpiredRetention(today);
        for (Document doc : expired) {
            doc.unlockDeletion();
            log.info("Retention expired for document {} ({}), unlocked for deletion",
                    doc.getId(), doc.getFileName());
        }

        // Flag approaching expiry (next 90 days)
        List<Document> approaching = documentRepository.findApproachingExpiry(
                today, today.plusDays(90));
        if (!approaching.isEmpty()) {
            log.info("{} documents approaching retention expiry within 90 days", approaching.size());
        }
    }

    static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
