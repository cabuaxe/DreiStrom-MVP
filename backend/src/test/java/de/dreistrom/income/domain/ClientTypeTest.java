package de.dreistrom.income.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTypeTest {

    @Test
    void hasExpectedValues() {
        assertThat(ClientType.values()).containsExactly(ClientType.B2B, ClientType.B2C);
    }
}
