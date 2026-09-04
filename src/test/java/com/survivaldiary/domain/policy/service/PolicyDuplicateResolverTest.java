package com.survivaldiary.domain.policy.service;

import com.survivaldiary.domain.policy.client.dto.YouthPolicyItem;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDuplicateResolverTest {
    private final PolicyDuplicateResolver resolver = new PolicyDuplicateResolver();

    static List<YouthPolicyItem> mentoringItems() {
        return List.of(new ObjectMapper().readValue(
                PolicyDuplicateResolverTest.class.getResourceAsStream(
                        "/fixtures/policy/duplicated-mentoring.json"),
                YouthPolicyItem[].class));
    }

    @Test
    void 실제_조선대_중복_등록은_최신_마감본을_선택한다() {
        var items = mentoringItems();
        assertThat(resolver.reconcile(items, Set.of())).containsExactly(items.get(1));
        assertThat(PolicyDuplicateResolver.duplicateKey(items.get(0)))
                .isEqualTo(PolicyDuplicateResolver.duplicateKey(items.get(1)));
    }

    @Test
    void 다음_조회에서_오래된_상시본이_나와도_최신_상태를_유지한다() {
        var items = mentoringItems();
        resolver.reconcile(List.of(items.get(1)), Set.of());
        assertThat(resolver.reconcile(List.of(items.get(0)), Set.of()))
                .containsExactly(items.get(1));
    }

    @Test
    void 오래된_등록을_먼저_읽어도_새로운_등록이_오면_교체한다() {
        var items = mentoringItems();
        resolver.reconcile(List.of(items.get(0)), Set.of());
        assertThat(resolver.reconcile(List.of(items.get(1)), Set.of()))
                .containsExactly(items.get(1));
    }

    @Test
    void 최신_재모집을_과거_마감보다_우선하므로_마감을_영구_고정하지_않는다() {
        var closed = item("closed", "https://example.org/program/1", "2026-07-01 10:00:00", "0057003");
        var reopened = item("reopened", "https://example.org/program/1", "2026-08-01 10:00:00", "0057002");
        assertThat(resolver.reconcile(List.of(closed, reopened), Set.of()))
                .containsExactly(reopened);
    }

    @Test
    void 이름만_같거나_홈페이지와_로그인_주소만_같으면_합치지_않는다() {
        for (String url : List.of("https://example.org/", "https://example.org/index.do",
                "https://example.org/login?returnUrl=apply")) {
            assertThat(resolver.reconcile(List.of(
                    item("a", url, "2026-07-01 10:00:00", "0057002"),
                    item("b", url, "2026-08-01 10:00:00", "0057003")
            ), Set.of())).hasSize(2);
        }
        assertThat(resolver.reconcile(List.of(
                item("a", "https://example.org/program/1", "2026-07-01 10:00:00", "0057002"),
                item("b", "https://example.org/program/2", "2026-08-01 10:00:00", "0057003")
        ), Set.of())).hasSize(2);
    }

    @Test
    void 수정_시각이_없거나_잘못됐으면_최신본을_추측하지_않는다() {
        var unknown = item("a", "https://example.org/program/3", null, "0057002");
        var invalid = item("b", "https://example.org/program/3", "2026-02-30 10:00:00", "0057003");
        assertThat(PolicyDuplicateResolver.duplicateKey(unknown)).isNull();
        assertThat(PolicyDuplicateResolver.sourceUpdatedAt(invalid)).isNull();
        assertThat(resolver.reconcile(List.of(unknown, invalid), Set.of())).hasSize(2);
    }

    @Test
    void 같은_조회에서_숨긴_등록의_중복본도_제외한다() {
        var items = mentoringItems();
        assertThat(resolver.reconcile(items, Set.of(items.get(0).plcyNo()))).isEmpty();
    }

    private YouthPolicyItem item(String id, String url, String updatedAt, String periodCode) {
        return new YouthPolicyItem(id, "동일 정책명", "설명", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                periodCode, null, null, null, url, null, null, null, null, updatedAt);
    }
}
