package de.dreistrom.common.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * JPA converter: BIGINT cents in the database <-> BigDecimal euros in Java.
 * Uses RoundingMode.HALF_UP with 2 decimal places.
 */
@Converter(autoApply = false)
public class MoneyConverter implements AttributeConverter<BigDecimal, Long> {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public Long convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.multiply(HUNDRED)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long dbData) {
        if (dbData == null) {
            return null;
        }
        return new BigDecimal(dbData)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
