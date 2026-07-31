package com.survivaldiary.domain.policy.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 온통청년 정책 API의 외부 응답 DTO.
 * 모든 필드는 공식 문서의 String 계약을 보존하며 내부 API DTO로 직접 노출하지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YouthPolicyItem(
        String plcyNo,
        String plcyNm,
        String plcyExplnCn,
        String lclsfNm,
        String mclsfNm,
        String plcyKywdNm,
        String plcySprtCn,
        String zipCd,
        String sprtTrgtMinAge,
        String sprtTrgtMaxAge,
        String sprtTrgtAgeLmtYn,
        String jobCd,
        String schoolCd,
        String earnCndSeCd,
        String earnMinAmt,
        String earnMaxAmt,
        String earnEtcCn,
        String aplyPrdSeCd,
        String aplyYmd,
        String plcyAplyMthdCn,
        String sbmsnDcmntCn,
        String aplyUrlAddr,
        String refUrlAddr1,
        String refUrlAddr2,
        String sprvsnInstCdNm,
        String operInstCdNm,
        String lastMdfcnDt
) {
}
