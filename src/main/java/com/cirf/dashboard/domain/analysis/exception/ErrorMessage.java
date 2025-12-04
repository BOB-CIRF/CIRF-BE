package com.cirf.dashboard.domain.analysis.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    LOG_NOT_FOUND("해당 로그를 찾을 수 없습니다."),
    INVALID_DATE_RANGE("잘못된 기간 범위입니다. 시작 시간은 종료 시간보다 이전이어야 합니다."),
    DATE_RANGE_TOO_LARGE("조회 기간이 너무 큽니다. 최대 조회 기간을 초과했습니다."),
    UNAUTHORIZED_CASE_ACCESS("해당 사례에 대한 접근 권한이 없습니다."),
    ELASTICSEARCH_COMMUNICATION_ERROR("Elasticsearch 통신 중 오류가 발생했습니다."),
    LOG_DOWNLOAD_FAILED("로그 다운로드 처리 중 오류가 발생했습니다."),
    FAILED_DECOMPRESS("파일 다운로드 및 압축 해제에 실패했습니다.");

    private final String message;
}
