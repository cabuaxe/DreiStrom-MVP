package de.dreistrom.onboarding.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.onboarding.domain.*;
import de.dreistrom.onboarding.dto.DecisionPointResponse;
import de.dreistrom.onboarding.dto.OnboardingProgressResponse;
import de.dreistrom.onboarding.dto.StepResponse;
import de.dreistrom.onboarding.mapper.OnboardingMapper;
import de.dreistrom.onboarding.mapper.OnboardingMapperImpl;
import de.dreistrom.onboarding.repository.DecisionPointRepository;
import de.dreistrom.onboarding.repository.RegistrationStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private RegistrationStepRepository stepRepository;

    @Mock
    private DecisionPointRepository decisionPointRepository;

    @Mock
    private AuditLogService auditLogService;

    private OnboardingMapper onboardingMapper = new OnboardingMapperImpl();

    private OnboardingService service;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        service = new OnboardingService(stepRepository, decisionPointRepository,
                onboardingMapper, auditLogService);
        testUser = new AppUser("test@example.com", "hash", "Test User");
        // Use reflection to set ID for test
        try {
            var idField = AppUser.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void initializeChecklist_creates15Steps() {
        when(stepRepository.countByUserId(1L)).thenReturn(0L);
        when(stepRepository.save(any(RegistrationStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(decisionPointRepository.save(any(DecisionPoint.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.initializeChecklist(testUser);

        ArgumentCaptor<RegistrationStep> captor = ArgumentCaptor.forClass(RegistrationStep.class);
        verify(stepRepository, times(15)).save(captor.capture());

        List<RegistrationStep> saved = captor.getAllValues();
        assertThat(saved).hasSize(15);
        assertThat(saved.get(0).getStepNumber()).isEqualTo(1);
        assertThat(saved.get(0).getTitle()).isEqualTo("ELSTER-Registrierung");
        assertThat(saved.get(14).getStepNumber()).isEqualTo(15);
        assertThat(saved.get(14).getTitle()).isEqualTo("GoBD-Aufbewahrung sicherstellen");
    }

    @Test
    void initializeChecklist_skipsIfAlreadyExists() {
        when(stepRepository.countByUserId(1L)).thenReturn(15L);

        service.initializeChecklist(testUser);

        verify(stepRepository, never()).save(any());
    }

    @Test
    void initializeChecklist_createsDecisionPointForStep4() {
        when(stepRepository.countByUserId(1L)).thenReturn(0L);
        when(stepRepository.save(any(RegistrationStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(decisionPointRepository.save(any(DecisionPoint.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.initializeChecklist(testUser);

        ArgumentCaptor<DecisionPoint> dpCaptor = ArgumentCaptor.forClass(DecisionPoint.class);
        verify(decisionPointRepository).save(dpCaptor.capture());

        DecisionPoint dp = dpCaptor.getValue();
        assertThat(dp.getQuestion()).contains("Kleinunternehmerregelung");
        assertThat(dp.getRecommendation()).isEqualTo(DecisionChoice.OPTION_B);
    }

    @Test
    void getProgress_calculatesCorrectPercentage() {
        RegistrationStep step1 = new RegistrationStep(testUser, 1, "Step 1", "desc", Responsible.USER, null);
        step1.start();
        step1.complete();
        RegistrationStep step2 = new RegistrationStep(testUser, 2, "Step 2", "desc", Responsible.USER, null);

        when(stepRepository.findByUserIdOrderByStepNumber(1L)).thenReturn(List.of(step1, step2));
        when(decisionPointRepository.findByStepIdIn(anyList())).thenReturn(List.of());

        OnboardingProgressResponse progress = service.getProgress(1L);

        assertThat(progress.totalSteps()).isEqualTo(2);
        assertThat(progress.completedSteps()).isEqualTo(1);
        assertThat(progress.progressPercent()).isEqualTo(50);
    }

    @Test
    void startStep_succeeds_whenNoDependencies() {
        RegistrationStep step = new RegistrationStep(testUser, 1, "ELSTER", "desc", Responsible.USER, null);
        setStepId(step, 10L);

        when(stepRepository.findByUserIdAndStepNumber(1L, 1)).thenReturn(Optional.of(step));
        when(decisionPointRepository.findByStepId(10L)).thenReturn(List.of());

        StepResponse response = service.startStep(1L, 1);

        assertThat(response.status()).isEqualTo(StepStatus.IN_PROGRESS);
    }

    @Test
    void startStep_succeeds_whenDependenciesCompleted() {
        RegistrationStep dep = new RegistrationStep(testUser, 1, "Dep", "desc", Responsible.USER, null);
        dep.start();
        dep.complete();

        RegistrationStep step = new RegistrationStep(testUser, 3, "Step3", "desc", Responsible.USER, "[1]");
        setStepId(step, 20L);

        when(stepRepository.findByUserIdAndStepNumber(1L, 3)).thenReturn(Optional.of(step));
        when(stepRepository.findByUserIdAndStepNumber(1L, 1)).thenReturn(Optional.of(dep));
        when(decisionPointRepository.findByStepId(20L)).thenReturn(List.of());

        StepResponse response = service.startStep(1L, 3);

        assertThat(response.status()).isEqualTo(StepStatus.IN_PROGRESS);
    }

    @Test
    void startStep_fails_whenDependencyNotCompleted() {
        RegistrationStep dep = new RegistrationStep(testUser, 1, "Dep", "desc", Responsible.USER, null);

        RegistrationStep step = new RegistrationStep(testUser, 3, "Step3", "desc", Responsible.USER, "[1]");
        setStepId(step, 20L);

        when(stepRepository.findByUserIdAndStepNumber(1L, 3)).thenReturn(Optional.of(step));
        when(stepRepository.findByUserIdAndStepNumber(1L, 1)).thenReturn(Optional.of(dep));

        assertThatThrownBy(() -> service.startStep(1L, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("depends on step 1");
    }

    @Test
    void completeStep_setsCompletedAt() {
        RegistrationStep step = new RegistrationStep(testUser, 1, "Step", "desc", Responsible.USER, null);
        step.start();
        setStepId(step, 10L);

        when(stepRepository.findByUserIdAndStepNumber(1L, 1)).thenReturn(Optional.of(step));
        when(decisionPointRepository.findByStepId(10L)).thenReturn(List.of());

        StepResponse response = service.completeStep(1L, 1);

        assertThat(response.status()).isEqualTo(StepStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void makeDecision_recordsChoice() {
        RegistrationStep step = new RegistrationStep(testUser, 4, "KUR", "desc", Responsible.USER, null);
        setStepId(step, 10L);
        DecisionPoint dp = new DecisionPoint(step, "Question?", "A", "B",
                DecisionChoice.OPTION_B, "reason");
        setDecisionPointId(dp, 100L);

        when(decisionPointRepository.findById(100L)).thenReturn(Optional.of(dp));

        DecisionPointResponse response = service.makeDecision(1L, 100L, DecisionChoice.OPTION_A);

        assertThat(response.userChoice()).isEqualTo(DecisionChoice.OPTION_A);
        assertThat(response.decidedAt()).isNotNull();
    }

    @Test
    void stepResponsibleTypes_areCorrect() {
        when(stepRepository.countByUserId(1L)).thenReturn(0L);
        when(stepRepository.save(any(RegistrationStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(decisionPointRepository.save(any(DecisionPoint.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.initializeChecklist(testUser);

        ArgumentCaptor<RegistrationStep> captor = ArgumentCaptor.forClass(RegistrationStep.class);
        verify(stepRepository, times(15)).save(captor.capture());

        List<RegistrationStep> saved = captor.getAllValues();
        // Step 10 (Buchhaltungssoftware) → SYSTEM
        assertThat(saved.get(9).getResponsible()).isEqualTo(Responsible.SYSTEM);
        // Step 13 (Steuerberater) → STEUERBERATER
        assertThat(saved.get(12).getResponsible()).isEqualTo(Responsible.STEUERBERATER);
        // Step 8 (Arbeitgeber) → USER
        assertThat(saved.get(7).getResponsible()).isEqualTo(Responsible.USER);
    }

    private void setStepId(RegistrationStep step, Long id) {
        try {
            var idField = RegistrationStep.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(step, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setDecisionPointId(DecisionPoint dp, Long id) {
        try {
            var idField = DecisionPoint.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(dp, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
