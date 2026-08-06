package com.survivaldiary.domain.policy.controller;

import com.survivaldiary.domain.policy.dto.HiddenPolicyResponse;
import com.survivaldiary.domain.policy.service.HiddenPolicyService;
import com.survivaldiary.global.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HiddenPolicyControllerTest {

    private HiddenPolicyService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(HiddenPolicyService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HiddenPolicyController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 관심_없음_목록을_페이징_형식으로_조회한다() throws Exception {
        HiddenPolicyResponse item = response();
        when(service.list(7L, 0, 20)).thenReturn(
                new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1)
        );

        mockMvc.perform(get("/api/users/me/hidden-policies"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.content[0].policyId").value("POLICY-1"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void 정책을_관심_없음으로_저장한다() throws Exception {
        when(service.hide(eq(7L), eq("POLICY-1"), any())).thenReturn(response());

        mockMvc.perform(put("/api/users/me/hidden-policies/POLICY-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "청년 주거 지원",
                                  "category": "주거",
                                  "shortSummary": "월세를 지원해요"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyId").value("POLICY-1"));
    }

    @Test
    void 관심_없음_정책을_복구한다() throws Exception {
        mockMvc.perform(delete("/api/users/me/hidden-policies/POLICY-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).restore(7L, "POLICY-1");
    }

    private HiddenPolicyResponse response() {
        return new HiddenPolicyResponse(
                "POLICY-1",
                "청년 주거 지원",
                "주거",
                "월세를 지원해요",
                LocalDateTime.of(2026, 8, 6, 12, 0)
        );
    }
}
