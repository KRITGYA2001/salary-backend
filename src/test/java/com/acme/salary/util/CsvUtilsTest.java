package com.acme.salary.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvUtilsTest {

    @Test
    void leavesPlainTextUntouchedAndNullEmpty() {
        assertThat(CsvUtils.escapeText("Asha Rao")).isEqualTo("Asha Rao");
        assertThat(CsvUtils.escapeText(null)).isEmpty();
    }

    @Test
    void quotesCellsWithDelimitersQuotesOrLineBreaks() {
        assertThat(CsvUtils.escapeText("Rao, Asha")).isEqualTo("\"Rao, Asha\"");
        assertThat(CsvUtils.escapeText("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvUtils.escapeText("a\nb")).isEqualTo("\"a\nb\"");
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        assertThat(CsvUtils.escapeText("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(CsvUtils.escapeText("@cmd")).isEqualTo("'@cmd");
    }

    @Test
    void joinsCellsIntoCrLfTerminatedLine() {
        assertThat(CsvUtils.toLine(List.of("a", "b"))).isEqualTo("a,b\r\n");
    }
}
