package com.cirf.dashboard.domain.scan.service;

import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
import com.cirf.dashboard.domain.scan.dto.response.ScanResultsResponse;
import com.cirf.dashboard.domain.scan.entity.ScanLogsMetadata;
import com.cirf.dashboard.domain.scan.entity.ScanRegionStatus;
import com.cirf.dashboard.domain.scan.exception.RegionNotFoundException;
import com.cirf.dashboard.domain.scan.exception.ScanNotCompletedException;
import com.cirf.dashboard.domain.scan.exception.ScanNotFoundException;
import com.cirf.dashboard.domain.scan.repository.ScanLogsRepository;
import com.cirf.dashboard.domain.scan.service.enums.LogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScanLogsService 테스트")
class ScanLogsServiceTest {

    @Mock
    private ScanLogsRepository scanLogsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ScanLogsService scanLogsService;

    private ScanLogsMetadata testMetadata;
    private ScanRegionStatus testRegionStatus;

    @BeforeEach
    void setUp() {
        testMetadata = ScanLogsMetadata.builder()
                .scanId(1L)
                .userId(1L)
                .caseId(100L)
                .accountId("123456789012")
                .status("SUCCEEDED")
                .build();

        testRegionStatus = ScanRegionStatus.builder()
                .pk("SCAN#1")
                .sk("REG#us-east-1")
                .region("us-east-1")
                .status("SUCCEEDED")
                .build();
    }

    @Nested
    @DisplayName("getScanResultsByRegion 테스트")
    class GetScanResultsByRegionTest {

        @Test
        @DisplayName("정상적으로 스캔 결과를 조회한다")
        void getScanResultsByRegion_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    10
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanLogsRepository.findLatestScan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanLogsRepository.findScanRegionStatus("SCAN#1", "REG#us-east-1"))
                    .thenReturn(Optional.of(testRegionStatus));
            when(scanLogsRepository.existsEnabledLog(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(true);

            // when
            Slice<ScanResultsResponse> result = scanLogsService.getScanResultsByRegion(1L, 100L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            verify(userRepository).existsById(1L);
            verify(scanLogsRepository).findLatestScan(1L, 100L, "123456789012");
        }

        @Test
        @DisplayName("region 없이 전체 스캔 결과를 조회한다")
        void getScanResultsByRegion_NoRegion_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    null,
                    0,
                    10
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanLogsRepository.findLatestScan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanLogsRepository.existsEnabledLog(anyLong(), anyString(), isNull(), anyString()))
                    .thenReturn(true);

            // when
            Slice<ScanResultsResponse> result = scanLogsService.getScanResultsByRegion(1L, 100L, request);

            // then
            assertThat(result).isNotNull();
            verify(scanLogsRepository, never()).findScanRegionStatus(anyString(), anyString());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 조회 시 예외 발생")
        void getScanResultsByRegion_UserNotFound_ThrowsException() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    10
            );

            when(userRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> scanLogsService.getScanResultsByRegion(999L, 100L, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("스캔 메타데이터가 없을 때 예외 발생")
        void getScanResultsByRegion_ScanNotFound_ThrowsException() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    10
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanLogsRepository.findLatestScan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scanLogsService.getScanResultsByRegion(1L, 100L, request))
                    .isInstanceOf(ScanNotFoundException.class);
        }

        @Test
        @DisplayName("스캔이 완료되지 않았을 때 예외 발생")
        void getScanResultsByRegion_ScanNotCompleted_ThrowsException() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    10
            );

            ScanLogsMetadata inProgressMetadata = ScanLogsMetadata.builder()
                    .scanId(1L)
                    .status("IN_PROGRESS")
                    .build();

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanLogsRepository.findLatestScan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(inProgressMetadata));

            // when & then
            assertThatThrownBy(() -> scanLogsService.getScanResultsByRegion(1L, 100L, request))
                    .isInstanceOf(ScanNotCompletedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 region으로 조회 시 예외 발생")
        void getScanResultsByRegion_RegionNotFound_ThrowsException() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-west-1",
                    0,
                    10
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanLogsRepository.findLatestScan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanLogsRepository.findScanRegionStatus("SCAN#1", "REG#us-west-1"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scanLogsService.getScanResultsByRegion(1L, 100L, request))
                    .isInstanceOf(RegionNotFoundException.class);
        }

        @Test
        @DisplayName("페이징이 정상적으로 작동한다")
        void getScanResultsByRegion_Pagination_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    5
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanLogsRepository.findLatestScan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanLogsRepository.findScanRegionStatus("SCAN#1", "REG#us-east-1"))
                    .thenReturn(Optional.of(testRegionStatus));
            when(scanLogsRepository.existsEnabledLog(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(true);

            // when
            Slice<ScanResultsResponse> result = scanLogsService.getScanResultsByRegion(1L, 100L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent().size()).isLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("모든 로그 타입에 대한 결과를 반환한다")
        void getScanResultsByRegion_AllLogTypes_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    30
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanLogsRepository.findLatestScan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanLogsRepository.findScanRegionStatus("SCAN#1", "REG#us-east-1"))
                    .thenReturn(Optional.of(testRegionStatus));
            when(scanLogsRepository.existsEnabledLog(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(true);

            // when
            Slice<ScanResultsResponse> result = scanLogsService.getScanResultsByRegion(1L, 100L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSizeLessThanOrEqualTo(LogType.values().length);
        }
    }
}