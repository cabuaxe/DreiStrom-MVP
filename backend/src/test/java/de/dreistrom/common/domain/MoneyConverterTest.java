package de.dreistrom.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyConverterTest {

    private final MoneyConverter converter = new MoneyConverter();

    @ParameterizedTest
    @CsvSource({
            "10.00,  1000",
            "0.01,   1",
            "0.00,   0",
            "999.99, 99999",
            "100.50, 10050"
    })
    void toDatabaseColumn_convertsEurosToCents(String euros, long expectedCents) {
        Long result = converter.convertToDatabaseColumn(new BigDecimal(euros));
        assertThat(result).isEqualTo(expectedCents);
    }

    @ParameterizedTest
    @CsvSource({
            "1000,  10.00",
            "1,     0.01",
            "0,     0.00",
            "99999, 999.99",
            "10050, 100.50"
    })
    void toEntityAttribute_convertsCentsToEuros(long cents, String expectedEuros) {
        BigDecimal result = converter.convertToEntityAttribute(cents);
        assertThat(result).isEqualByComparingTo(new BigDecimal(expectedEuros));
    }

    @Test
    void toDatabaseColumn_roundsHalfUp() {
        // 10.005 -> 1000.5 -> rounds to 1001
        Long result = converter.convertToDatabaseColumn(new BigDecimal("10.005"));
        assertThat(result).isEqualTo(1001L);
    }

    @Test
    void toDatabaseColumn_null_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void toEntityAttribute_null_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void roundTrip_isLossless() {
        BigDecimal original = new BigDecimal("12345.67");
        Long cents = converter.convertToDatabaseColumn(original);
        BigDecimal restored = converter.convertToEntityAttribute(cents);
        assertThat(restored).isEqualByComparingTo(original);
    }
}
