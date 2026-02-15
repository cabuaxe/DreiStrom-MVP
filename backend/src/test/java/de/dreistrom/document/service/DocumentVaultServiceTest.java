package de.dreistrom.document.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.document.domain.Document;
import de.dreistrom.document.domain.DocumentType;
import de.dreistrom.document.repository.DocumentRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentVaultServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private DocumentVaultService service;

    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");

    @Nested
    class Upload {
        @Test
        void uploadsDocumentSuccessfully() {
            byte[] content = "test content".getBytes();
            when(documentRepository.findByUserIdAndSha256Hash(any(), anyString()))
                    .thenReturn(Optional.empty());
            when(s3StorageService.upload(any(), anyString(), anyString(), any()))
                    .thenReturn("documents/1/uuid/test.pdf");
            when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Document result = service.upload(user, "test.pdf", "application/pdf",
                    content, DocumentType.INVOICE, "Test invoice");

            assertThat(result.getFileName()).isEqualTo("test.pdf");
            assertThat(result.getDocumentType()).isEqualTo(DocumentType.INVOICE);
            assertThat(result.getRetentionYears()).isEqualTo(10);
            assertThat(result.isDeletionLocked()).isTrue();
            verify(s3StorageService).upload(any(), eq("test.pdf"), eq("application/pdf"), eq(content));
            verify(documentRepository).save(any());
        }

        @Test
        void rejectsDuplicateHash() {
            byte[] content = "duplicate".getBytes();
            Document existing = new Document(user, "existing.pdf", "application/pdf",
                    100, "key", "hash", DocumentType.INVOICE, null);

            when(documentRepository.findByUserIdAndSha256Hash(any(), anyString()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.upload(user, "new.pdf", "application/pdf",
                    content, DocumentType.INVOICE, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate document");

            verify(s3StorageService, never()).upload(any(), anyString(), anyString(), any());
        }
    }

    @Nested
    class Delete {
        @Test
        void blocksDeletionDuringRetention() {
            // Document with default retention (locked, future retention)
            Document doc = new Document(user, "invoice.pdf", "application/pdf",
                    100, "key", "hash", DocumentType.INVOICE, null);

            when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> service.delete(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("retention period")
                    .hasMessageContaining("§147 AO");

            verify(s3StorageService, never()).delete(anyString());
        }
    }

    @Nested
    class Sha256 {
        @Test
        void computesCorrectHash() {
            byte[] data = "hello world".getBytes();
            String hash = DocumentVaultService.computeSha256(data);

            // Known SHA-256 of "hello world"
            assertThat(hash).isEqualTo(
                    "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
        }

        @Test
        void differentContentProducesDifferentHash() {
            String hash1 = DocumentVaultService.computeSha256("content1".getBytes());
            String hash2 = DocumentVaultService.computeSha256("content2".getBytes());

            assertThat(hash1).isNotEqualTo(hash2);
        }
    }

    @Nested
    class RetentionPeriods {
        @Test
        void invoiceHas10YearRetention() {
            assertThat(DocumentType.INVOICE.retentionYears()).isEqualTo(10);
        }

        @Test
        void correspondenceHas6YearRetention() {
            assertThat(DocumentType.CORRESPONDENCE.retentionYears()).isEqualTo(6);
        }

        @Test
        void contractHas6YearRetention() {
            assertThat(DocumentType.CONTRACT.retentionYears()).isEqualTo(6);
        }

        @Test
        void bankStatementHas10YearRetention() {
            assertThat(DocumentType.BANK_STATEMENT.retentionYears()).isEqualTo(10);
        }
    }

    @Nested
    class ProcessRetention {
        @Test
        void processesExpiredDocuments() {
            Document doc = new Document(user, "old.pdf", "application/pdf",
                    100, "key", "hash", DocumentType.INVOICE, null);

            when(documentRepository.findExpiredRetention(any())).thenReturn(List.of(doc));
            when(documentRepository.findApproachingExpiry(any(), any())).thenReturn(List.of());

            service.processRetentionExpiry();

            verify(documentRepository).findExpiredRetention(any());
            verify(documentRepository).findApproachingExpiry(any(), any());
        }
    }
}
