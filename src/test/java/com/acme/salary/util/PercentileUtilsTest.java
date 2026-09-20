package com.acme.salary.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PercentileUtilsTest {

    @Test
    void medianOfOddSizedListIsTheMiddleValue() {
        assertThat(PercentileUtils.median(List.of(50, 60, 70, 80, 90))).isEqualTo(70);
    }

    @Test
    void medianOfEvenSizedListIsTheLowerMiddleValue() {
        assertThat(PercentileUtils.median(List.of(10, 20, 30, 40))).isEqualTo(20);
    }

    @Test
    void singleValueIsEveryPercentile() {
        assertThat(PercentileUtils.nearestRank(List.of(42), 1)).isEqualTo(42);
        assertThat(PercentileUtils.nearestRank(List.of(42), 90)).isEqualTo(42);
    }

    @Test
    void ninetiethPercentileOfTenValuesIsTheNinthValue() {
        List<Integer> oneToTen = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertThat(PercentileUtils.nearestRank(oneToTen, 90)).isEqualTo(9);
    }

    @Test
    void hundredthPercentileIsTheMaximum() {
        assertThat(PercentileUtils.nearestRank(List.of(3, 5, 8), 100)).isEqualTo(8);
    }

    @Test
    void handlesDuplicateValues() {
        assertThat(PercentileUtils.median(List.of(5, 5, 5, 9))).isEqualTo(5);
    }

    @Test
    void rejectsEmptyListAndOutOfRangePercentile() {
        assertThatThrownBy(() -> PercentileUtils.median(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PercentileUtils.nearestRank(List.of(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PercentileUtils.nearestRank(List.of(1), 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
