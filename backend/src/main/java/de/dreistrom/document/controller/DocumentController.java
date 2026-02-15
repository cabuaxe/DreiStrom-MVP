package de.dreistrom.document.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.document.domain.DocumentType;
import de.dreistrom.document.dto.DocumentResponse;
import de.dreistrom.document.service.DocumentVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentVaultService documentVaultService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @AuthenticationPrincipal AppUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) String description) throws IOException {
        return DocumentResponse.from(
                documentVaultService.upload(
                        user,
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes(),
                        documentType,
                        description));
    }

    @GetMapping
    public List<DocumentResponse> list(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(required = false) DocumentType type) {
        if (type != null) {
            return documentVaultService.listByUserAndType(user.getId(), type).stream()
                    .map(DocumentResponse::from)
                    .toList();
        }
        return documentVaultService.listByUser(user.getId()).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(@PathVariable Long id) {
        return DocumentResponse.from(documentVaultService.getById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Void> download(@PathVariable Long id) {
        URL url = documentVaultService.getDownloadUrl(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.toString()))
                .build();
    }

    @PatchMapping("/{id}")
    public DocumentResponse updateMetadata(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags) {
        return DocumentResponse.from(
                documentVaultService.updateMetadata(id, description, tags));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        documentVaultService.delete(id);
    }
}
