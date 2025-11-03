package com.cirf.dashboard.domain.scan.service;

import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
import com.cirf.dashboard.domain.scan.dto.response.ScanCompletedResponse;
import com.cirf.dashboard.domain.scan.dto.response.ScanEc2Response;
import com.cirf.dashboard.domain.scan.entity.EnabledInstances;
import com.cirf.dashboard.domain.scan.entity.ScanEc2Metadata;
import com.cirf.dashboard.domain.scan.exception.InvalidRegionException;
import com.cirf.dashboard.domain.scan.exception.NotFoundEc2MetadataException;
import com.cirf.dashboard.domain.scan.exception.ScanEc2MetadataCreationException;
import com.cirf.dashboard.domain.scan.repository.ScanEc2Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScanEc2Service 테스트")
class ScanEc2ServiceTest {

    @Mock
    private ScanEc2Repository scanEc2Repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AsyncScanEc2Service asyncScanEc2Service;

    @InjectMocks
    private ScanEc2Service scanEc2Service;

    private ScanEc2Metadata testMetadata;
    private EnabledInstances testInstance;

    @BeforeEach
    void setUp() {
        testMetadata = ScanEc2Metadata.builder()
                .scanId(1L)
                .userId(1L)
                .caseId(100L)
                .accountId("123456789012")
                .status("IN_PROGRESS")
                .build();

        testInstance = EnabledInstances.builder()
                .idxId(1L)
                .scanId(1L)
                .instanceId("i-1234567890abcdef0")
                .instanceName("test-instance")
                .instanceType("t2.micro")
                .region("us-east-1")
                .status("running")
                .platformDetails("Linux/UNIX")
                .publicIp("54.123.45.67")
                .build();
    }

    @Nested
    @DisplayName("scanEc2Request 테스트")
    class ScanEc2RequestTest {

        @Test
        @DisplayName("정상적으로 EC2 스캔을 요청한다")
        void scanEc2Request_Success() {
            // given
            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.createScanEc2Metadata(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            doNothing().when(asyncScanEc2Service).saveScanEc2Instances(1L);

            // when
            ScanCompletedResponse response = scanEc2Service.scanEc2Request(1L, 100L, "123456789012");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getScanId()).isEqualTo(1L);

            verify(userRepository).existsById(1L);
            verify(scanEc2Repository).createScanEc2Metadata(1L, 100L, "123456789012");
            verify(asyncScanEc2Service).saveScanEc2Instances(1L);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 스캔 요청 시 예외 발생")
        void scanEc2Request_UserNotFound_ThrowsException() {
            // given
            when(userRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> scanEc2Service.scanEc2Request(999L, 100L, "123456789012"))
                    .isInstanceOf(UserNotFoundException.class);

            verify(scanEc2Repository, never()).createScanEc2Metadata(anyLong(), anyLong(), anyString());
            verify(asyncScanEc2Service, never()).saveScanEc2Instances(anyLong());
        }

        @Test
        @DisplayName("메타데이터 생성 실패 시 예외 발생")
        void scanEc2Request_MetadataCreationFailed_ThrowsException() {
            // given
            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.createScanEc2Metadata(1L, 100L, "123456789012"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scanEc2Service.scanEc2Request(1L, 100L, "123456789012"))
                    .isInstanceOf(ScanEc2MetadataCreationException.class);

            verify(asyncScanEc2Service, never()).saveScanEc2Instances(anyLong());
        }
    }

    @Nested
    @DisplayName("getEc2Lists 테스트")
    class GetEc2ListsTest {

        @Test
        @DisplayName("정상적으로 EC2 인스턴스 목록을 조회한다")
        void getEc2Lists_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    10
            );

            List<EnabledInstances> instances = Collections.singletonList(testInstance);

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.findLatestEc2Scan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanEc2Repository.getEc2InstancesByRegion(1L, "us-east-1"))
                    .thenReturn(instances);

            // when
            Slice<ScanEc2Response> result = scanEc2Service.getEc2Lists(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            ScanEc2Response response = result.getContent().get(0);
            assertThat(response.instanceId()).isEqualTo("i-1234567890abcdef0");
            assertThat(response.instanceName()).isEqualTo("test-instance");
            assertThat(response.region()).isEqualTo("us-east-1");

            verify(userRepository).existsById(1L);
            verify(scanEc2Repository).findLatestEc2Scan(1L, 100L, "123456789012");
            verify(scanEc2Repository).getEc2InstancesByRegion(1L, "us-east-1");
        }

        @Test
        @DisplayName("region 없이 모든 인스턴스를 조회한다")
        void getEc2Lists_AllRegions_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    null,
                    0,
                    10
            );

            List<EnabledInstances> instances = Arrays.asList(testInstance, testInstance);

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.findLatestEc2Scan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanEc2Repository.getEc2InstancesByRegion(1L, null))
                    .thenReturn(instances);

            // when
            Slice<ScanEc2Response> result = scanEc2Service.getEc2Lists(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 조회 시 예외 발생")
        void getEc2Lists_UserNotFound_ThrowsException() {
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
            assertThatThrownBy(() -> scanEc2Service.getEc2Lists(999L, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("메타데이터가 없을 때 예외 발생")
        void getEc2Lists_MetadataNotFound_ThrowsException() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    10
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.findLatestEc2Scan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scanEc2Service.getEc2Lists(1L, request))
                    .isInstanceOf(NotFoundEc2MetadataException.class);
        }

        @Test
        @DisplayName("유효하지 않은 region으로 조회 시 예외 발생")
        void getEc2Lists_InvalidRegion_ThrowsException() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "invalid-region",
                    0,
                    10
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.findLatestEc2Scan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));

            // when & then
            assertThatThrownBy(() -> scanEc2Service.getEc2Lists(1L, request))
                    .isInstanceOf(InvalidRegionException.class);
        }

        @Test
        @DisplayName("페이징이 정상적으로 작동한다")
        void getEc2Lists_Pagination_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    5
            );

            List<EnabledInstances> instances = Arrays.asList(
                    testInstance, testInstance, testInstance,
                    testInstance, testInstance, testInstance
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.findLatestEc2Scan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanEc2Repository.getEc2InstancesByRegion(1L, "us-east-1"))
                    .thenReturn(instances);

            // when
            Slice<ScanEc2Response> result = scanEc2Service.getEc2Lists(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(5);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("빈 인스턴스 목록을 정상적으로 반환한다")
        void getEc2Lists_EmptyList_Success() {
            // given
            ScanResultsRequest request = new ScanResultsRequest(
                    100L,
                    "123456789012",
                    "us-east-1",
                    0,
                    10
            );

            when(userRepository.existsById(1L)).thenReturn(true);
            when(scanEc2Repository.findLatestEc2Scan(1L, 100L, "123456789012"))
                    .thenReturn(Optional.of(testMetadata));
            when(scanEc2Repository.getEc2InstancesByRegion(1L, "us-east-1"))
                    .thenReturn(Collections.emptyList());

            // when
            Slice<ScanEc2Response> result = scanEc2Service.getEc2Lists(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }
    }
}