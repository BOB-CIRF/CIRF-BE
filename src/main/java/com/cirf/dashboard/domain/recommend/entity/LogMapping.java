package com.cirf.dashboard.domain.recommend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * Behavior에 포함되는 로그 매핑 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class LogMapping {
    private String logType;         // 예: "CLOUDTRAIL", "S3_ACCESS_LOGS"
    private String displayName;     // 예: "CloudTrail", "S3 Access Logs"
    private String reason;          // 로그 수집 근거
}
