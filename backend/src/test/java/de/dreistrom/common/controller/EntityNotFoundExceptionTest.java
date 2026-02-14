package de.dreistrom.common.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNotFoundExceptionTest {

    @Test
    void constructor_formatsMessage_withEntityNameAndId() {
        EntityNotFoundException ex = new EntityNotFoundException("Invoice", 42L);
        assertThat(ex.getMessage()).isEqualTo("Invoice not found with id: 42");
    }

    @Test
    void constructor_handlesStringId() {
        EntityNotFoundException ex = new EntityNotFoundException("Document", "abc-123");
        assertThat(ex.getMessage()).isEqualTo("Document not found with id: abc-123");
    }
}
