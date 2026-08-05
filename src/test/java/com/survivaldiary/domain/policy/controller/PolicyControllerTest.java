package com.survivaldiary.domain.policy.controller;

import com.survivaldiary.domain.policy.dto.PolicyCategory;
import com.survivaldiary.domain.policy.dto.PolicyDetail;
import com.survivaldiary.domain.policy.dto.PolicySearchResponse;
import com.survivaldiary.domain.policy.service.PolicyRecommendationService;
import com.survivaldiary.domain.policy.service.PolicyService;
import com.survivaldiary.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyControllerTest {

    private PolicyService policyService;
    private PolicyRecommendationService policyRecommendationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        policyService = mock(PolicyService.class);
        policyRecommendationService = mock(PolicyRecommendationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PolicyController(policyService, policyRecommendationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 저장된_사용자_조건으로_추천_목록을_조회한다() throws Exception {
        when(policyRecommendationService.recommend(any(), any())).thenReturn(
                new PolicySearchResponse(List.of(), false, 1, null)
        );

        mockMvc.perform(post("/api/policies/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "HOUSING",
                                  "keyword": " 월세 ",
                                  "page": 1,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void 맞춤_조건은_POST_JSON으로_받고_부분_결과_정보를_반환한다() throws Exception {
        when(policyService.search(any())).thenReturn(
                new PolicySearchResponse(List.of(), true, 1, 2)
        );

        mockMvc.perform(post("/api/policies/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "age": 27,
                                  "regionCode": "11",
                                  "districtCode": "11680",
                                  "employmentStatus": "JOB_SEEKING",
                                  "incomeRange": "BELOW_100",
                                  "category": "HOUSING",
                                  "keyword": "월세",
                                  "interests": ["HOUSING", "ASSET_BUILDING"],
                                  "page": 1,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.partialResult").value(true))
                .andExpect(jsonPath("$.data.checkedProviderPages").value(1))
                .andExpect(jsonPath("$.data.nextPage").value(2));
    }

    @Test
    void 필수_맞춤_조건이_없으면_공통_검증_오류를_반환한다() throws Exception {
        mockMvc.perform(post("/api/policies/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionCode": "11",
                                  "employmentStatus": "JOB_SEEKING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 알_수_없는_관심_주제는_검색_전에_거절한다() throws Exception {
        mockMvc.perform(post("/api/policies/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "age": 27,
                                  "regionCode": "11",
                                  "interests": ["UNKNOWN_INTEREST"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 문자열_정책_ID로_상세를_조회한다() throws Exception {
        when(policyService.findDetail("POLICY-A")).thenReturn(
                new PolicyDetail(
                        "POLICY-A",
                        "주거",
                        PolicyCategory.HOUSING,
                        "청년 주거 정책",
                        "정책 설명",
                        null,
                        "지원 내용",
                        "20260701~20260731",
                        java.time.LocalDate.of(2026, 7, 31),
                        "지원 대상",
                        "주관 기관",
                        "운영 기관",
                        "온라인 신청",
                        List.of(),
                        null,
                        List.of()
                )
        );

        mockMvc.perform(get("/api/policies/POLICY-A"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.policyId").value("POLICY-A"))
                .andExpect(jsonPath("$.data.applicationEndDate").value("2026-07-31"))
                .andExpect(jsonPath("$.data.supportAmount").doesNotExist());
    }
}
