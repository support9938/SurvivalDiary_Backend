package com.survivaldiary.domain.policy.controller;

import com.survivaldiary.domain.policy.dto.PolicyPreferenceResponse;
import com.survivaldiary.domain.policy.service.PolicyPreferenceService;
import com.survivaldiary.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyPreferenceControllerTest {

    private PolicyPreferenceService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PolicyPreferenceService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PolicyPreferenceController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 저장된_조건이_없으면_정상_응답으로_saved_false를_반환한다() throws Exception {
        when(service.get(7L)).thenReturn(PolicyPreferenceResponse.empty(26));

        mockMvc.perform(get("/api/users/me/policy-preferences"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.saved").value(false))
                .andExpect(jsonPath("$.data.age").value(26))
                .andExpect(jsonPath("$.data.regionCode").doesNotExist());
    }

    @Test
    void 선택_조건을_생략해_기본_조건을_저장할_수_있다() throws Exception {
        when(service.save(eq(7L), any())).thenReturn(
                new PolicyPreferenceResponse(
                        true,
                        26,
                        "11",
                        null,
                        "JOB_SEEKING",
                        null,
                        null,
                        "UNEMPLOYED",
                        true,
                        null,
                        java.util.Set.of("EMPLOYMENT", "ASSET_BUILDING")
                )
        );

        mockMvc.perform(put("/api/users/me/policy-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionCode": "11",
                                  "workStatus": "UNEMPLOYED",
                                  "jobSeeking": true,
                                  "interests": ["EMPLOYMENT", "ASSET_BUILDING"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.districtCode").doesNotExist())
                .andExpect(jsonPath("$.data.incomeRange").doesNotExist())
                .andExpect(jsonPath("$.data.category").doesNotExist())
                .andExpect(jsonPath("$.data.workStatus").value("UNEMPLOYED"))
                .andExpect(jsonPath("$.data.jobSeeking").value(true))
                .andExpect(jsonPath("$.data.interests.length()").value(2));
    }

    @Test
    void 필수_조건이_없으면_공통_검증_오류를_반환한다() throws Exception {
        mockMvc.perform(put("/api/users/me/policy-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(7L, null);
    }
}
