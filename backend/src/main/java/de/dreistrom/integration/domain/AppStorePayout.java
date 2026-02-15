package de.dreistrom.integration.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.MoneyConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "appstore_payout")
@Getter
@NoArgsConstructor
public class AppStorePayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutPlatform platform;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false, length = 10)
    private String region;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "gross_revenue_cents", nullable = false)
    @Convert(converter = MoneyConverter.class)
    private BigDecimal grossRevenue;

    @Column(name = "commission_cents", nullable = false)
    @Convert(converter = MoneyConverter.class)
    private BigDecimal commission;

    @Column(name = "net_revenue_cents", nullable = false)
    @Convert(converter = MoneyConverter.class)
    private BigDecimal netRevenue;

    @Column(name = "vat_cents", nullable = false)
    @Convert(converter = MoneyConverter.class)
    private BigDecimal vat;

    @Column(name = "product_id", length = 500)
    private String productId;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "import_batch_id", nullable = false, length = 100)
    private String importBatchId;

    @Column(name = "raw_csv_line", columnDefinition = "TEXT")
    private String rawCsvLine;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    public AppStorePayout(AppUser user, PayoutPlatform platform, LocalDate reportDate,
                          String region, String currency,
                          BigDecimal grossRevenue, BigDecimal commission,
                          BigDecimal netRevenue, BigDecimal vat,
                          String productId, String productName, int quantity,
                          String importBatchId, String rawCsvLine) {
        this.user = user;
        this.platform = platform;
        this.reportDate = reportDate;
        this.region = region;
        this.currency = currency;
        this.grossRevenue = grossRevenue;
        this.commission = commission;
        this.netRevenue = netRevenue;
        this.vat = vat;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.importBatchId = importBatchId;
        this.rawCsvLine = rawCsvLine;
        this.createdAt = Instant.now();
    }
}
