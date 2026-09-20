package com.acme.salary.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlUtilsTest {

    @Test
    void escapesLikeWildcardsAndBackslash() {
        assertThat(SqlUtils.escapeLike("50%_off\\")).isEqualTo("50\\%\\_off\\\\");
    }

    @Test
    void buildsTrimmedPrefixPattern() {
        assertThat(SqlUtils.prefixPattern("  ann_ ")).isEqualTo("ann\\_%");
    }
}
