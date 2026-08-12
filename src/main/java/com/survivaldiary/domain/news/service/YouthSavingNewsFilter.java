package com.survivaldiary.domain.news.service;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class YouthSavingNewsFilter {

    private static final List<String> YOUTH_KEYWORDS = List.of(
            "청년",
            "대학생",
            "취업준비생",
            "취준생",
            "사회초년생",
            "자취생",
            "1인 가구",
            "1인가구",
            "2030세대",
            "20대",
            "30대"
    );

    private static final List<String> SAVING_KEYWORDS = List.of(
            "절약",
            "생활비",
            "물가",
            "할인",
            "쿠폰",
            "환급",
            "지원금",
            "지원 사업",
            "지원사업",
            "금융 지원",
            "주거비",
            "월세",
            "전세",
            "보증금",
            "식비",
            "교통비",
            "통신비",
            "공공요금",
            "고정비",
            "가계부",
            "예산",
            "저축",
            "적금",
            "재테크",
            "자산 형성",
            "자산형성",
            "금리",
            "대출 이자",
            "이자 부담",
            "소득공제",
            "세액공제",
            "절세",
            "장학금",
            "청년도약계좌",
            "청년내일저축계좌"
    );

    private static final List<String> POLITICAL_KEYWORDS = List.of(
            "대통령",
            "대통령실",
            "국회",
            "국회의원",
            "정당",
            "여당",
            "야당",
            "당대표",
            "원내대표",
            "민주당",
            "국민의힘",
            "조국혁신당",
            "개혁신당",
            "정치",
            "정치권",
            "의회",
            "시의원",
            "도의원",
            "구의원",
            "선거",
            "총선",
            "대선",
            "지방선거",
            "후보",
            "공천",
            "지지율",
            "탄핵",
            "정쟁",
            "특검"
    );

    public boolean isRelevant(String title, String summary) {
        String content = safe(title) + " " + safe(summary);
        return containsAny(content, YOUTH_KEYWORDS)
                && containsAny(content, SAVING_KEYWORDS)
                && !containsAny(content, POLITICAL_KEYWORDS);
    }

    private boolean containsAny(String content, List<String> keywords) {
        return keywords.stream().anyMatch(content::contains);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
