package de.dreistrom.income.dto;

/**
 * Progressive disclosure feature flags computed from the user's actual business volume.
 * The UI adapts to the user's complexity: low-volume users see a simplified view,
 * complex features surface only when relevant metrics approach their triggers.
 * <p>
 * Per architecture doc §4.2.4: flags are computed by the threshold engine
 * and exposed through a dedicated Angular service.
 */
public record UserFeatureFlags(
        int year,

        // ── Stream activity ────────────────────────────────────────────
        boolean hasEmploymentIncome,
        boolean hasFreiberufIncome,
        boolean hasGewerbeIncome,
        boolean hasMultipleStreams,

        // ── Dashboard cards ────────────────────────────────────────────
        boolean showKleinunternehmerCard,
        boolean showAbfaerbungCard,
        boolean showGewerbesteuerCard,
        boolean showBilanzierungWarnings,
        boolean showSocialInsuranceCard,
        boolean showMandatoryFilingCard,
        boolean showArbZGCard,

        // ── Module visibility ──────────────────────────────────────────
        boolean showInvoicingModule,
        boolean showExpenseAllocation,
        boolean showVorauszahlungen,
        boolean showTaxEstimation,

        // ── Advanced features ──────────────────────────────────────────
        boolean showOssRules,
        boolean showZmReporting,

        // ── Complexity level: 1=micro, 2=small, 3=medium ──────────────
        int complexityLevel
) {}
