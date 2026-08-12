package com.survivaldiary.domain.policy.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEducationClassifierTest {

    @Test
    void 이삼년제와_사년제_대학은_같은_온통청년_대학_코드로_비교한다() {
        assertThat(PolicyEducationClassifier.classify(
                "0049005",
                "COLLEGE_2_3_YEAR",
                "ENROLLED"
        )).isEqualTo(PolicyEducationClassifier.EducationMatch.MATCHED);
        assertThat(PolicyEducationClassifier.classify(
                "0049005",
                "UNIVERSITY_4_YEAR",
                "ENROLLED"
        )).isEqualTo(PolicyEducationClassifier.EducationMatch.MATCHED);
    }

    @Test
    void 제한없음_코드는_입력한_교육_단계와_무관하게_허용한다() {
        assertThat(PolicyEducationClassifier.classify(
                "0049010",
                "HIGH_SCHOOL",
                "GRADUATED"
        )).isEqualTo(PolicyEducationClassifier.EducationMatch.UNRESTRICTED);
    }

    @Test
    void 휴학은_온통청년의_독립_코드가_없어_확인_필요로_판정한다() {
        assertThat(PolicyEducationClassifier.classify(
                "0049005",
                "UNIVERSITY_4_YEAR",
                "ON_LEAVE"
        )).isEqualTo(PolicyEducationClassifier.EducationMatch.UNKNOWN);
    }
}
