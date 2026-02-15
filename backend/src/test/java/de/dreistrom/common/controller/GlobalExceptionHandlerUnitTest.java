package de.dreistrom.common.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_returns400_withMessage() {
        ProblemDetail result = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid amount"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Bad Request");
        assertThat(result.getDetail()).isEqualTo("Invalid amount");
    }

    @Test
    void handleAuthentication_returns401() {
        ProblemDetail result = handler.handleAuthentication(
                new BadCredentialsException("Bad credentials"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("Unauthorized");
        assertThat(result.getDetail()).isEqualTo("Authentication required");
    }

    @Test
    void handleAccessDenied_returns403() {
        ProblemDetail result = handler.handleAccessDenied(
                new AccessDeniedException("Forbidden"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(result.getTitle()).isEqualTo("Forbidden");
        assertThat(result.getDetail()).isEqualTo("Access denied");
    }

    @Test
    void handleNotFound_returns404_withEntityMessage() {
        ProblemDetail result = handler.handleNotFound(
                new EntityNotFoundException("Invoice", 42L));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Not Found");
        assertThat(result.getDetail()).isEqualTo("Invoice not found with id: 42");
    }

    @SuppressWarnings("unchecked")
    @Test
    void handleConstraintViolation_returns400_withFieldErrors() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("amount");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be positive");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ProblemDetail result = handler.handleConstraintViolation(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation Error");
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) result.getProperties().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("amount", "must be positive");
    }
}
