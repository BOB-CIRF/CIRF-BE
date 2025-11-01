package com.cirf.dashboard.domain.cases.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 공통 응답 형식
 * @param <T> 응답 데이터 타입
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMessage<T> {

    private Integer status;
    private String message;
    private T data;

    /**
     * 성공 응답 생성
     */
    public static <T> ResponseMessage<T> success(String message, T data) {
        return ResponseMessage.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 에러 응답 생성
     */
    public static <T> ResponseMessage<T> error(Integer status, String message) {
        return ResponseMessage.<T>builder()
                .status(status)
                .message(message)
                .data(null)
                .build();
    }
}