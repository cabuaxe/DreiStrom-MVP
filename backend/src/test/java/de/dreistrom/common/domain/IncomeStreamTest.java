package de.dreistrom.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeStreamTest {

    @Test
    void hasThreeStreams() {
        assertThat(IncomeStream.values()).hasSize(3);
    }

    @Test
    void enumValues_matchExpected() {
        assertThat(IncomeStream.valueOf("EMPLOYMENT")).isEqualTo(IncomeStream.EMPLOYMENT);
        assertThat(IncomeStream.valueOf("FREIBERUF")).isEqualTo(IncomeStream.FREIBERUF);
        assertThat(IncomeStream.valueOf("GEWERBE")).isEqualTo(IncomeStream.GEWERBE);
    }
}
