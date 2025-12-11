package com.cirf.dashboard.domain.recommend.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseMessage {
    GET_CATEGORIES_SUCCESS("카테고리 목록 조회 성공"),
    GET_BEHAVIORS_SUCCESS("세부 행위 목록 조회 성공"),
    GET_BEHAVIOR_DETAIL_SUCCESS("세부 행위 상세 조회 성공"),
    GET_RECOMMEND_SUCCESS("수집 추천 로그 조회 성공");

    private final String message;
}
