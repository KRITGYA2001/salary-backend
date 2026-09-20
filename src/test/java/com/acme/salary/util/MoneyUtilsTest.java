package com.acme.salary.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyUtilsTest {

    @Test
    void convertsMajorAmountToMinorUnitsForTwoDecimalCurrency() {
        assertThat(MoneyUtils.toMinorUnits(new BigDecimal("12345.67"), 2)).isEqualTo(1_234_567L);
    }

    @Test
    void keepsWholeAmountForZeroDecimalCurrency() {
        assertThat(MoneyUtils.toMinorUnits(new BigDecimal("5000000"), 0)).isEqualTo(5_000_000L);
    }

    @Test
    void roundsHalfUpToCurrencyPrecision() {
        assertThat(MoneyUtils.toMinorUnits(new BigDecimal("10.005"), 2)).isEqualTo(1001L);
        assertThat(MoneyUtils.toMinorUnits(new BigDecimal("10.004"), 2)).isEqualTo(1000L);
    }

    @Test
    void avoidsBinaryFloatingPointDrift() {
        BigDecimal pointThirty = new BigDecimal("0.1").add(new BigDecimal("0.2"));

        assertThat(MoneyUtils.toMinorUnits(pointThirty, 2)).isEqualTo(30L);
    }

    @Test
    void rejectsZeroNegativeAndNullAmounts() {
        assertThatThrownBy(() -> MoneyUtils.toMinorUnits(BigDecimal.ZERO, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtils.toMinorUnits(new BigDecimal("-1"), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtils.toMinorUnits(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsMinorUnitsBackToMajorAmountWithScale() {
        assertThat(MoneyUtils.toMajorUnits(1_234_567L, 2)).isEqualByComparingTo("12345.67");
        assertThat(MoneyUtils.toMajorUnits(1_234_567L, 2).scale()).isEqualTo(2);
        assertThat(MoneyUtils.toMajorUnits(5_000_000L, 0)).isEqualByComparingTo("5000000");
    }

    @Test
    void roundsUsdAmountsHalfUpToCents() {
        assertThat(MoneyUtils.roundUsd(12.345)).isEqualByComparingTo("12.35");
        assertThat(MoneyUtils.roundUsd(1000.0).scale()).isEqualTo(2);
    }
}
