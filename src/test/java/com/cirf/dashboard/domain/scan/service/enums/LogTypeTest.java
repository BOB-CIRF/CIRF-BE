package com.cirf.dashboard.domain.scan.service.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LogType Enum 테스트")
class LogTypeTest {

    @Test
    @DisplayName("모든 로그 타입이 정의되어 있다")
    void logType_AllTypesAreDefined() {
        // when
        LogType[] logTypes = LogType.values();

        // then
        assertThat(logTypes).isNotEmpty();
        assertThat(logTypes.length).isGreaterThan(10);
    }

    @ParameterizedTest
    @EnumSource(LogType.class)
    @DisplayName("각 로그 타입이 null이 아닌 type 값을 가진다")
    void logType_HasNonNullType(LogType logType) {
        // then
        assertThat(logType.getType()).isNotNull();
        assertThat(logType.getType()).isNotBlank();
    }

    @Test
    @DisplayName("CloudTrail 로그 타입이 올바른 값을 가진다")
    void logType_CloudTrail_HasCorrectValue() {
        // when
        LogType cloudTrail = LogType.CLOUDTRAIL;

        // then
        assertThat(cloudTrail.getType()).isEqualTo("cloudtrail");
    }

    @Test
    @DisplayName("VPC 플로우 로그 타입이 올바른 값을 가진다")
    void logType_VPC_HasCorrectValue() {
        // when
        LogType vpc = LogType.VPC;

        // then
        assertThat(vpc.getType()).isEqualTo("vpc flow logs");
    }

    @Test
    @DisplayName("S3 액세스 로그 타입이 올바른 값을 가진다")
    void logType_S3Access_HasCorrectValue() {
        // when
        LogType s3Access = LogType.S3_ACCESS;

        // then
        assertThat(s3Access.getType()).isEqualTo("s3 access logs");
    }

    @Test
    @DisplayName("모든 로그 타입의 type 값이 유니크하다")
    void logType_AllTypesAreUnique() {
        // when
        List<String> types = Arrays.stream(LogType.values())
                .map(LogType::getType)
                .collect(Collectors.toList());

        // then
        assertThat(types).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("주요 AWS 로그 타입들이 포함되어 있다")
    void logType_ContainsMajorAwsLogTypes() {
        // when
        List<String> types = Arrays.stream(LogType.values())
                .map(LogType::getType)
                .collect(Collectors.toList());

        // then
        assertThat(types).contains(
                "cloudtrail",
                "vpc flow logs",
                "s3 access logs",
                "elb",
                "cloudfront",
                "waf",
                "config"
        );
    }

    @Test
    @DisplayName("네트워크 관련 로그 타입들이 포함되어 있다")
    void logType_ContainsNetworkRelatedLogs() {
        // when
        List<String> types = Arrays.stream(LogType.values())
                .map(LogType::getType)
                .collect(Collectors.toList());

        // then
        assertThat(types).contains(
                "vpc flow logs",
                "network firewall alert logs",
                "network firewall flow logs",
                "transit gateway flow logs"
        );
    }

    @Test
    @DisplayName("데이터베이스 관련 로그 타입들이 포함되어 있다")
    void logType_ContainsDatabaseRelatedLogs() {
        // when
        List<String> types = Arrays.stream(LogType.values())
                .map(LogType::getType)
                .collect(Collectors.toList());

        // then
        assertThat(types).contains(
                "rds audit",
                "redshift audit"
        );
    }

    @Test
    @DisplayName("보안 관련 로그 타입들이 포함되어 있다")
    void logType_ContainsSecurityRelatedLogs() {
        // when
        List<String> types = Arrays.stream(LogType.values())
                .map(LogType::getType)
                .collect(Collectors.toList());

        // then
        assertThat(types).contains(
                "cloudtrail",
                "waf",
                "network firewall alert logs",
                "session manager logs"
        );
    }

    @Test
    @DisplayName("enum name과 type 값이 의미적으로 일치한다")
    void logType_NameMatchesTypeSemantics() {
        // given & when & then
        assertThat(LogType.CLOUDTRAIL.name()).isEqualTo("CLOUDTRAIL");
        assertThat(LogType.VPC.name()).isEqualTo("VPC");
        assertThat(LogType.S3_ACCESS.name()).isEqualTo("S3_ACCESS");
        assertThat(LogType.ELB.name()).isEqualTo("ELB");
        assertThat(LogType.WAF.name()).isEqualTo("WAF");
    }
}