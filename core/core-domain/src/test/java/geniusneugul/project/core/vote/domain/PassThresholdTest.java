package geniusneugul.project.core.vote.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PassThresholdTest {

    @DisplayName("2명 이하는 전원이 찬성해야, 3명 이상은 과반수(절반 초과)가 찬성해야 통과한다.")
    @ParameterizedTest(name = "{0}명 중 {1}명 찬성 → {2}")
    @CsvSource({
            "1, 1, true",
            "2, 1, false",
            "2, 2, true",
            "3, 1, false",
            "3, 2, true",
            "4, 2, false",
            "4, 3, true",
            "5, 3, true"
    })
    void isMet(int participants, int approvals, boolean expected) {
        // when
        boolean met = PassThreshold.isMet(approvals, participants);

        // then
        assertThat(met).isEqualTo(expected);
    }
}
