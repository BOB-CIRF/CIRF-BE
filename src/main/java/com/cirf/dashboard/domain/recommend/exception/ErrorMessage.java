package com.cirf.dashboard.domain.recommend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {
    CATEGORY_NOT_FOUND("카테고리를 찾을 수 없습니다."),
    BEHAVIOR_NOT_FOUND("세부 행위를 찾을 수 없습니다."),
    INVALID_CATEGORY_ID("유효하지 않은 카테고리 ID입니다."),
    INVALID_BEHAVIOR_ID("유효하지 않은 세부 행위 ID입니다."),
    EMPTY_BEHAVIOR_IDS("세부 행위 ID 목록이 비어있습니다.");

    private final String message;
}
