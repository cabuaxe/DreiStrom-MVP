package de.dreistrom.onboarding.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.onboarding.domain.*;
import de.dreistrom.onboarding.dto.DecisionPointResponse;
import de.dreistrom.onboarding.dto.OnboardingProgressResponse;
import de.dreistrom.onboarding.dto.StepResponse;
import de.dreistrom.onboarding.mapper.OnboardingMapper;
import de.dreistrom.onboarding.repository.DecisionPointRepository;
import de.dreistrom.onboarding.repository.RegistrationStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final RegistrationStepRepository stepRepository;
    private final DecisionPointRepository decisionPointRepository;
    private final OnboardingMapper onboardingMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public void initializeChecklist(AppUser user) {
        long existing = stepRepository.countByUserId(user.getId());
        if (existing > 0) {
            return;
        }

        createStep(user, 1, "ELSTER-Registrierung",
                "Register at ELSTER and request activation code (1-2 weeks by post).",
                Responsible.USER, null);

        createStep(user, 2, "Gewerbeanmeldung",
                "Complete the Gewerbeanmeldung at the Berlin Bezirksamt for software product sales.",
                Responsible.USER, null);

        RegistrationStep step3 = createStep(user, 3, "Fragebogen zur steuerlichen Erfassung",
                "Submit the Fragebogen zur steuerlichen Erfassung via ELSTER, declaring both Freiberuf and Gewerbe activities (§138 AO).",
                Responsible.USER, "[1]");

        RegistrationStep step4 = createStep(user, 4, "Kleinunternehmerregelung entscheiden",
                "Decide on the Kleinunternehmerregelung (§19 UStG), considering expected revenue and client base.",
                Responsible.USER, "[3]");

        decisionPointRepository.save(new DecisionPoint(
                step4,
                "Soll die Kleinunternehmerregelung (§19 UStG) angewendet werden?",
                "Ja — keine USt auf Rechnungen, kein Vorsteuerabzug, einfachere Buchhaltung",
                "Nein — USt wird ausgewiesen, Vorsteuerabzug möglich, professioneller bei B2B-Kunden",
                DecisionChoice.OPTION_B,
                "Bei B2B-Kunden (App Store, Freelance-Aufträge) ist Regelbesteuerung vorteilhaft: Vorsteuerabzug auf Betriebsausgaben und professionelleres Auftreten."
        ));

        createStep(user, 5, "USt-IdNr beantragen",
                "Apply for a USt-IdNr at the Bundeszentralamt für Steuern if required for B2B transactions with Apple/Google or EU clients.",
                Responsible.USER, "[4]");

        createStep(user, 6, "Geschäftskonto eröffnen",
                "Open a dedicated business bank account for all self-employed income and expenses.",
                Responsible.USER, null);

        createStep(user, 7, "Auszahlungskonto konfigurieren",
                "Configure the business account as the payout destination in App Store Connect and Google Play Console.",
                Responsible.USER, "[6]");

        createStep(user, 8, "Arbeitgeber informieren",
                "Notify the employer (Kita Casa Azul) in writing regarding Nebentätigkeit.",
                Responsible.USER, null);

        createStep(user, 9, "Krankenkasse informieren",
                "Notify the Krankenkasse about additional self-employed income (§206 SGB V).",
                Responsible.USER, null);

        createStep(user, 10, "Buchhaltungssoftware einrichten",
                "Set up GoBD-compliant accounting software with separate categories for Freiberuf and Gewerbe.",
                Responsible.SYSTEM, null);

        createStep(user, 11, "Rechnungsvorlagen erstellen",
                "Create compliant invoice templates (§14 UStG), with separate numbering sequences for freelance and product sales.",
                Responsible.USER, "[4]");

        createStep(user, 12, "Steuerrücklage einrichten",
                "Establish a tax reserve — set aside 25-35% of net self-employed profit monthly.",
                Responsible.USER, "[6]");

        createStep(user, 13, "Steuerberater konsultieren",
                "Consult a Steuerberater, especially for the initial setup (cost is tax-deductible under §4 Abs. 4 EStG).",
                Responsible.STEUERBERATER, null);

        createStep(user, 14, "Einkommensteuererklärung",
                "File the annual Einkommensteuererklärung with Anlage N, S, G, EÜR, and Vorsorgeaufwand.",
                Responsible.USER, "[3,10]");

        createStep(user, 15, "GoBD-Aufbewahrung sicherstellen",
                "Maintain GoBD-compliant document retention for the prescribed periods (§147 AO).",
                Responsible.SYSTEM, "[10]");
    }

    private RegistrationStep createStep(AppUser user, int stepNumber, String title,
                                        String description, Responsible responsible,
                                        String dependencies) {
        RegistrationStep step = new RegistrationStep(user, stepNumber, title,
                description, responsible, dependencies);
        return stepRepository.save(step);
    }

    @Transactional(readOnly = true)
    public OnboardingProgressResponse getProgress(Long userId) {
        List<RegistrationStep> steps = stepRepository.findByUserIdOrderByStepNumber(userId);
        long total = steps.size();
        long completed = steps.stream().filter(s -> s.getStatus() == StepStatus.COMPLETED).count();
        long inProgress = steps.stream().filter(s -> s.getStatus() == StepStatus.IN_PROGRESS).count();
        long blocked = steps.stream().filter(s -> s.getStatus() == StepStatus.BLOCKED).count();
        int percent = total > 0 ? (int) ((completed * 100) / total) : 0;

        List<Long> stepIds = steps.stream().map(RegistrationStep::getId).toList();
        Map<Long, List<DecisionPointResponse>> decisionsByStep = decisionPointRepository
                .findByStepIdIn(stepIds)
                .stream()
                .map(onboardingMapper::toDecisionPointResponse)
                .collect(Collectors.groupingBy(DecisionPointResponse::stepId));

        List<StepResponse> stepResponses = steps.stream()
                .map(step -> onboardingMapper.toStepResponse(
                        step,
                        decisionsByStep.getOrDefault(step.getId(), List.of())))
                .toList();

        return new OnboardingProgressResponse(total, completed, inProgress, blocked, percent, stepResponses);
    }

    @Transactional
    public StepResponse startStep(Long userId, int stepNumber) {
        RegistrationStep step = stepRepository.findByUserIdAndStepNumber(userId, stepNumber)
                .orElseThrow(() -> new EntityNotFoundException("RegistrationStep", stepNumber));

        checkDependencies(userId, step);
        step.start();

        List<DecisionPointResponse> decisions = decisionPointRepository.findByStepId(step.getId())
                .stream()
                .map(onboardingMapper::toDecisionPointResponse)
                .toList();

        return onboardingMapper.toStepResponse(step, decisions);
    }

    @Transactional
    public StepResponse completeStep(Long userId, int stepNumber) {
        RegistrationStep step = stepRepository.findByUserIdAndStepNumber(userId, stepNumber)
                .orElseThrow(() -> new EntityNotFoundException("RegistrationStep", stepNumber));

        step.complete();

        List<DecisionPointResponse> decisions = decisionPointRepository.findByStepId(step.getId())
                .stream()
                .map(onboardingMapper::toDecisionPointResponse)
                .toList();

        return onboardingMapper.toStepResponse(step, decisions);
    }

    @Transactional
    public DecisionPointResponse makeDecision(Long userId, Long decisionPointId, DecisionChoice choice) {
        DecisionPoint dp = decisionPointRepository.findById(decisionPointId)
                .orElseThrow(() -> new EntityNotFoundException("DecisionPoint", decisionPointId));

        RegistrationStep step = dp.getStep();
        if (!step.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("DecisionPoint", decisionPointId);
        }

        dp.decide(choice);
        return onboardingMapper.toDecisionPointResponse(dp);
    }

    private void checkDependencies(Long userId, RegistrationStep step) {
        String deps = step.getDependencies();
        if (deps == null || deps.isBlank()) {
            return;
        }

        String cleaned = deps.replaceAll("[\\[\\]\\s]", "");
        if (cleaned.isEmpty()) {
            return;
        }

        for (String depStr : cleaned.split(",")) {
            int depStepNumber = Integer.parseInt(depStr.trim());
            RegistrationStep depStep = stepRepository.findByUserIdAndStepNumber(userId, depStepNumber)
                    .orElse(null);
            if (depStep != null && depStep.getStatus() != StepStatus.COMPLETED) {
                throw new IllegalStateException(
                        "Step " + step.getStepNumber() + " depends on step " + depStepNumber
                                + " which is not yet completed (status: " + depStep.getStatus() + ")");
            }
        }
    }
}
