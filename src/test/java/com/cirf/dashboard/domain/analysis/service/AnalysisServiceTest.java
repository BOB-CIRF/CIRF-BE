package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.dto.request.LogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.LogRawDataResponse;
import com.cirf.dashboard.domain.analysis.dto.response.LogStashResponse;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import com.cirf.dashboard.domain.analysis.exception.LogNotFoundException;
import com.cirf.dashboard.domain.analysis.repository.LogEventRepository;
import com.cirf.dashboard.domain.auth.entity.Tenant;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisService 테스트")
class AnalysisServiceTest {

    @Mock
    private LogEventRepository logEventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private User testUser;
    private Tenant testTenant;
    private LogEvent testLogEvent;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .id(1L)
                .tenantName("Test Tenant")
                .build();

        testUser = User.builder()
                .id(1L)
                .userName("Test User")
                .loginId("testuser")
                .tenant(testTenant)
                .build();

        testLogEvent = LogEvent.builder()
                .id("log-123")
                .tenantId("1")
                .caseId("100")
                .eventTime(LocalDateTime.now())
                .eventName("TestEvent")
                .sourceIpAddress("192.168.1.1")
                .userIdentityArn("arn:aws:iam::123456789012:user/testuser")
                .awsRegion("us-east-1")
                .build();
    }

    @Nested
    @DisplayName("queryLogs 테스트")
    class QueryLogsTest {

        @Test
        @DisplayName("정상적으로 로그를 조회한다")
        void queryLogs_Success() {
            // given
            LogQueryRequest request = LogQueryRequest.builder()
                    .caseId(100L)
                    .accountId("123456789012")
                    .startDate(LocalDateTime.now().minusDays(7))
                    .endDate(LocalDateTime.now())
                    .pageNumber(0)
                    .pageSize(10)
                    .build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<LogEvent> logEventPage = new PageImpl<>(
                    Collections.singletonList(testLogEvent),
                    pageable,
                    1
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(logEventRepository.searchByQuery("1", request)).thenReturn(logEventPage);

            // when
            Page<LogStashResponse> result = analysisService.queryLogs(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);

            LogStashResponse response = result.getContent().get(0);
            assertThat(response.getId()).isEqualTo("log-123");
            assertThat(response.getEventName()).isEqualTo("TestEvent");

            verify(userRepository).findById(1L);
            verify(logEventRepository).searchByQuery("1", request);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 조회 시 예외 발생")
        void queryLogs_UserNotFound_ThrowsException() {
            // given
            LogQueryRequest request = LogQueryRequest.builder()
                    .caseId(100L)
                    .build();

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> analysisService.queryLogs(999L, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(logEventRepository, never()).searchByQuery(anyString(), any());
        }

        @Test
        @DisplayName("빈 로그 목록을 정상적으로 반환한다")
        void queryLogs_EmptyResult_Success() {
            // given
            LogQueryRequest request = LogQueryRequest.builder()
                    .caseId(100L)
                    .pageNumber(0)
                    .pageSize(10)
                    .build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<LogEvent> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(logEventRepository.searchByQuery("1", request)).thenReturn(emptyPage);

            // when
            Page<LogStashResponse> result = analysisService.queryLogs(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("필터링된 조건으로 로그를 조회한다")
        void queryLogs_WithFilters_Success() {
            // given
            LogQueryRequest request = LogQueryRequest.builder()
                    .caseId(100L)
                    .accountId("123456789012")
                    .region("us-east-1")
                    .eventName("TestEvent")
                    .sourceIp("192.168.1.1")
                    .startDate(LocalDateTime.now().minusDays(1))
                    .endDate(LocalDateTime.now())
                    .pageNumber(0)
                    .pageSize(20)
                    .build();

            Pageable pageable = PageRequest.of(0, 20);
            Page<LogEvent> logEventPage = new PageImpl<>(
                    Collections.singletonList(testLogEvent),
                    pageable,
                    1
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(logEventRepository.searchByQuery("1", request)).thenReturn(logEventPage);

            // when
            Page<LogStashResponse> result = analysisService.queryLogs(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(logEventRepository).searchByQuery("1", request);
        }
    }

    @Nested
    @DisplayName("getRawLogData 테스트")
    class GetRawLogDataTest {

        @Test
        @DisplayName("정상적으로 원시 로그 데이터를 조회한다")
        void getRawLogData_Success() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(logEventRepository.findByIdWithTenant("1", "100", "log-123"))
                    .thenReturn(Optional.of(testLogEvent));

            // when
            LogRawDataResponse result = analysisService.getRawLogData(1L, 100L, "log-123");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("log-123");
            assertThat(result.getEventName()).isEqualTo("TestEvent");

            verify(userRepository).findById(1L);
            verify(logEventRepository).findByIdWithTenant("1", "100", "log-123");
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 조회 시 예외 발생")
        void getRawLogData_UserNotFound_ThrowsException() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> analysisService.getRawLogData(999L, 100L, "log-123"))
                    .isInstanceOf(UserNotFoundException.class);

            verify(logEventRepository, never()).findByIdWithTenant(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("존재하지 않는 로그 조회 시 예외 발생")
        void getRawLogData_LogNotFound_ThrowsException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(logEventRepository.findByIdWithTenant("1", "100", "invalid-log"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> analysisService.getRawLogData(1L, 100L, "invalid-log"))
                    .isInstanceOf(LogNotFoundException.class);
        }

        @Test
        @DisplayName("다른 테넌트의 로그 조회 시 예외 발생")
        void getRawLogData_DifferentTenant_ThrowsException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(logEventRepository.findByIdWithTenant("1", "100", "log-123"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> analysisService.getRawLogData(1L, 100L, "log-123"))
                    .isInstanceOf(LogNotFoundException.class);
        }
    }
}