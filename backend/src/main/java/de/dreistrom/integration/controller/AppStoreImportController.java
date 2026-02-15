package de.dreistrom.integration.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.integration.domain.PayoutPlatform;
import de.dreistrom.integration.dto.PayoutResponse;
import de.dreistrom.integration.repository.AppStorePayoutRepository;
import de.dreistrom.integration.service.ApplePayoutImporter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appstore")
@RequiredArgsConstructor
public class AppStoreImportController {

    private final ApplePayoutImporter appleImporter;
    private final AppStorePayoutRepository payoutRepository;

    @PostMapping("/apple/import")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PayoutResponse> importAppleCsv(
            @AuthenticationPrincipal AppUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean smallBusinessProgram) throws IOException {
        String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        return appleImporter.importCsv(user, csv, smallBusinessProgram).stream()
                .map(PayoutResponse::from)
                .toList();
    }

    @GetMapping("/payouts")
    public List<PayoutResponse> getPayouts(
            @AuthenticationPrincipal AppUser user,
            @RequestParam PayoutPlatform platform,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return payoutRepository
                .findByUserIdAndPlatformAndReportDateBetweenOrderByReportDateAsc(
                        user.getId(), platform, from, to)
                .stream()
                .map(PayoutResponse::from)
                .toList();
    }
}
