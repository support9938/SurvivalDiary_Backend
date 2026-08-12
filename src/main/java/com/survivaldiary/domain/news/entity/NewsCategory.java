package com.survivaldiary.domain.news.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NewsCategory {
    LIVING_ECONOMY("생활경제"),
    FINANCE("금융"),
    POLICY("정책"),
    SAVING("절약");

    private final String label;
}
