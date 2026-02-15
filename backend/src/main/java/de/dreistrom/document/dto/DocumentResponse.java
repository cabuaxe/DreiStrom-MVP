package de.dreistrom.document.dto;

import de.dreistrom.document.domain.Document;
import de.dreistrom.document.domain.DocumentType;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        String fileName,
        String contentType,
        long fileSize,
        String sha256Hash,
        DocumentType documentType,
        int retentionYears,
        LocalDate retentionUntil,
        boolean deletionLocked,
        String description,
        Instant uploadedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getSha256Hash(),
                doc.getDocumentType(),
                doc.getRetentionYears(),
                doc.getRetentionUntil(),
                doc.isDeletionLocked(),
                doc.getDescription(),
                doc.getUploadedAt()
        );
    }
}
