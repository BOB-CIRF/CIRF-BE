package com.cirf.dashboard.domain.cases.service;

import com.cirf.dashboard.domain.auth.entity.Tenant;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.cases.dto.request.CaseCreateRequest;
import com.cirf.dashboard.domain.cases.dto.request.CaseUpdateRequest;
import com.cirf.dashboard.domain.cases.dto.response.*;
import com.cirf.dashboard.domain.cases.entity.AccountId;
import com.cirf.dashboard.domain.cases.entity.CaseStatus;
import com.cirf.dashboard.domain.cases.entity.IncidentCase;
import com.cirf.dashboard.domain.cases.exception.AccessDeniedException;
import com.cirf.dashboard.domain.cases.repository.AccountIdRepository;
import com.cirf.dashboard.domain.cases.repository.IncidentCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaseService 테스트")
class CaseServiceTest {

    @Mock
    private IncidentCaseRepository incidentCaseRepository;

    @Mock
    private AccountIdRepository accountIdRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CaseService caseService;

    private User testUser;
    private Tenant testTenant;
    private IncidentCase testCase;
    private AccountId testAccountId;

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

        testAccountId = AccountId.builder()
                .id(1L)
                .accountId("123456789012")
                .roleArn("arn:aws:iam::123456789012:role/TestRole")
                .roleCheck(true)
                .build();

        testCase = IncidentCase.builder()
                .id(1L)
                .user(testUser)
                .caseName("Test Case")
                .description("Test Description")
                .status(CaseStatus.ACTIVE)
                .accountIdList(Collections.singletonList(testAccountId))
                .build();
    }

    @Nested
    @DisplayName("createCase 테스트")
    class CreateCaseTest {

        @Test
        @DisplayName("정상적으로 케이스를 생성한다")
        void createCase_Success() {
            // given
            CaseCreateRequest request = CaseCreateRequest.builder()
                    .caseName("New Case")
                    .caseDescription("New Description")
                    .accountIds(Arrays.asList("123456789012", "098765432109"))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(accountIdRepository.findByAccountId("123456789012")).thenReturn(Optional.of(testAccountId));
            when(accountIdRepository.findByAccountId("098765432109")).thenReturn(Optional.empty());
            when(incidentCaseRepository.save(any(IncidentCase.class))).thenAnswer(invocation -> {
                IncidentCase saved = invocation.getArgument(0);
                return IncidentCase.builder()
                        .id(2L)
                        .user(saved.getUser())
                        .caseName(saved.getCaseName())
                        .description(saved.getDescription())
                        .status(saved.getStatus())
                        .build();
            });
            when(accountIdRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            CaseCreateResponse response = caseService.createCase(1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCaseId()).isEqualTo(2L);
            verify(userRepository).findById(1L);
            verify(incidentCaseRepository).save(any(IncidentCase.class));
            verify(accountIdRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("유효하지 않은 userId로 케이스 생성 시 예외 발생")
        void createCase_InvalidUserId_ThrowsException() {
            // given
            CaseCreateRequest request = CaseCreateRequest.builder()
                    .caseName("New Case")
                    .caseDescription("New Description")
                    .accountIds(List.of("123456789012"))
                    .build();

            // when & then
            assertThatThrownBy(() -> caseService.createCase(null, request))
                    .isInstanceOf(AccessDeniedException.class);

            assertThatThrownBy(() -> caseService.createCase(0L, request))
                    .isInstanceOf(AccessDeniedException.class);

            assertThatThrownBy(() -> caseService.createCase(-1L, request))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 케이스 생성 시 예외 발생")
        void createCase_UserNotFound_ThrowsException() {
            // given
            CaseCreateRequest request = CaseCreateRequest.builder()
                    .caseName("New Case")
                    .caseDescription("New Description")
                    .accountIds(List.of("123456789012"))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> caseService.createCase(1L, request))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("빈 accountIds로 케이스 생성 시 예외 발생")
        void createCase_EmptyAccountIds_ThrowsException() {
            // given
            CaseCreateRequest request = CaseCreateRequest.builder()
                    .caseName("New Case")
                    .caseDescription("New Description")
                    .accountIds(Collections.emptyList())
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> caseService.createCase(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accountIds는 1개 이상이어야 합니다");
        }

        @Test
        @DisplayName("null accountIds로 케이스 생성 시 예외 발생")
        void createCase_NullAccountIds_ThrowsException() {
            // given
            CaseCreateRequest request = CaseCreateRequest.builder()
                    .caseName("New Case")
                    .caseDescription("New Description")
                    .accountIds(null)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> caseService.createCase(1L, request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getCaseList 테스트")
    class GetCaseListTest {

        @Test
        @DisplayName("정상적으로 케이스 목록을 조회한다")
        void getCaseList_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
            Page<IncidentCase> casePage = new PageImpl<>(
                    Collections.singletonList(testCase),
                    pageable,
                    1
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findByUserId(1L, pageable)).thenReturn(casePage);
            when(accountIdRepository.findByIncidentCaseId(1L))
                    .thenReturn(Collections.singletonList(testAccountId));

            // when
            CaseListResponse response = caseService.getCaseList(1L, 0, 10);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);

            CaseListResponse.CaseItem item = response.getContent().get(0);
            assertThat(item.getCaseId()).isEqualTo(1L);
            assertThat(item.getCaseName()).isEqualTo("Test Case");
            assertThat(item.getAnalystName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("유효하지 않은 userId로 목록 조회 시 예외 발생")
        void getCaseList_InvalidUserId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> caseService.getCaseList(null, 0, 10))
                    .isInstanceOf(AccessDeniedException.class);

            assertThatThrownBy(() -> caseService.getCaseList(0L, 0, 10))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 목록 조회 시 예외 발생")
        void getCaseList_UserNotFound_ThrowsException() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> caseService.getCaseList(999L, 0, 10))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("빈 목록을 정상적으로 반환한다")
        void getCaseList_EmptyList_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
            Page<IncidentCase> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findByUserId(1L, pageable)).thenReturn(emptyPage);

            // when
            CaseListResponse response = caseService.getCaseList(1L, 0, 10);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateCase 테스트")
    class UpdateCaseTest {

        @Test
        @DisplayName("정상적으로 케이스를 수정한다")
        void updateCase_Success() {
            // given
            CaseUpdateRequest request = CaseUpdateRequest.builder()
                    .caseName("Updated Case")
                    .caseDescription("Updated Description")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));

            // when
            CaseUpdateResponse response = caseService.updateCase(1L, 1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCaseId()).isEqualTo(1L);
            verify(incidentCaseRepository).findById(1L);
        }

        @Test
        @DisplayName("케이스 이름만 수정한다")
        void updateCase_OnlyName_Success() {
            // given
            CaseUpdateRequest request = CaseUpdateRequest.builder()
                    .caseName("Updated Name Only")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));

            // when
            CaseUpdateResponse response = caseService.updateCase(1L, 1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCaseId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("다른 사용자의 케이스 수정 시 예외 발생")
        void updateCase_DifferentUser_ThrowsException() {
            // given
            CaseUpdateRequest request = CaseUpdateRequest.builder()
                    .caseName("Updated Case")
                    .build();

            User differentUser = User.builder()
                    .id(2L)
                    .userName("Different User")
                    .tenant(testTenant)
                    .build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(differentUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));

            // when & then
            assertThatThrownBy(() -> caseService.updateCase(2L, 1L, request))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 케이스 수정 시 예외 발생")
        void updateCase_CaseNotFound_ThrowsException() {
            // given
            CaseUpdateRequest request = CaseUpdateRequest.builder()
                    .caseName("Updated Case")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> caseService.updateCase(1L, 999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 사례입니다");
        }

        @Test
        @DisplayName("accountIds를 함께 수정한다")
        void updateCase_WithAccountIds_Success() {
            // given
            List<String> newAccountIds = Arrays.asList("111111111111", "222222222222");
            CaseUpdateRequest request = CaseUpdateRequest.builder()
                    .caseName("Updated Case")
                    .accountIds(newAccountIds)
                    .build();

            AccountId newAccount1 = AccountId.builder()
                    .id(2L)
                    .accountId("111111111111")
                    .build();
            AccountId newAccount2 = AccountId.builder()
                    .id(3L)
                    .accountId("222222222222")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(accountIdRepository.countByAccountIdIn(newAccountIds)).thenReturn(2L);
            when(accountIdRepository.findByIncidentCaseId(1L))
                    .thenReturn(Collections.singletonList(testAccountId));
            when(accountIdRepository.findByAccountIdIn(newAccountIds))
                    .thenReturn(Arrays.asList(newAccount1, newAccount2));
            when(accountIdRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            CaseUpdateResponse response = caseService.updateCase(1L, 1L, request);

            // then
            assertThat(response).isNotNull();
            verify(accountIdRepository, times(2)).saveAll(anyList());
        }

        @Test
        @DisplayName("존재하지 않는 accountId로 수정 시 예외 발생")
        void updateCase_InvalidAccountIds_ThrowsException() {
            // given
            List<String> invalidAccountIds = Arrays.asList("111111111111", "invalid");
            CaseUpdateRequest request = CaseUpdateRequest.builder()
                    .accountIds(invalidAccountIds)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(accountIdRepository.countByAccountIdIn(invalidAccountIds)).thenReturn(1L);

            // when & then
            assertThatThrownBy(() -> caseService.updateCase(1L, 1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 accountId가 포함되어 있습니다");
        }
    }

    @Nested
    @DisplayName("deleteCase 테스트")
    class DeleteCaseTest {

        @Test
        @DisplayName("정상적으로 케이스를 삭제한다")
        void deleteCase_Success() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(accountIdRepository.findByIncidentCaseId(1L))
                    .thenReturn(Collections.singletonList(testAccountId));
            when(accountIdRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            CaseDeleteResponse response = caseService.deleteCase(1L, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCaseId()).isEqualTo(1L);
            verify(incidentCaseRepository).delete(testCase);
            verify(accountIdRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("다른 사용자의 케이스 삭제 시 예외 발생")
        void deleteCase_DifferentUser_ThrowsException() {
            // given
            User differentUser = User.builder()
                    .id(2L)
                    .userName("Different User")
                    .tenant(testTenant)
                    .build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(differentUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));

            // when & then
            assertThatThrownBy(() -> caseService.deleteCase(2L, 1L))
                    .isInstanceOf(AccessDeniedException.class);

            verify(incidentCaseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 케이스 삭제 시 예외 발생")
        void deleteCase_CaseNotFound_ThrowsException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> caseService.deleteCase(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 사례입니다");
        }

        @Test
        @DisplayName("유효하지 않은 userId로 삭제 시 예외 발생")
        void deleteCase_InvalidUserId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> caseService.deleteCase(null, 1L))
                    .isInstanceOf(AccessDeniedException.class);

            assertThatThrownBy(() -> caseService.deleteCase(0L, 1L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("getCaseDetail 테스트")
    class GetCaseDetailTest {

        @Test
        @DisplayName("정상적으로 케이스 상세를 조회한다")
        void getCaseDetail_Success() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));
            when(accountIdRepository.findByIncidentCaseId(1L))
                    .thenReturn(Collections.singletonList(testAccountId));

            // when
            CaseDetailResponse response = caseService.getCaseDetail(1L, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCaseId()).isEqualTo(1L);
            assertThat(response.getCaseName()).isEqualTo("Test Case");
            assertThat(response.getCaseDescription()).isEqualTo("Test Description");
            assertThat(response.getStatus()).isEqualTo("active");
            assertThat(response.getAnalystName()).isEqualTo("Test User");
            assertThat(response.getAccountIds()).hasSize(1);
            assertThat(response.getAccountIds().get(0)).isEqualTo("123456789012");
        }

        @Test
        @DisplayName("다른 사용자의 케이스 조회 시 예외 발생")
        void getCaseDetail_DifferentUser_ThrowsException() {
            // given
            User differentUser = User.builder()
                    .id(2L)
                    .userName("Different User")
                    .tenant(testTenant)
                    .build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(differentUser));
            when(incidentCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));

            // when & then
            assertThatThrownBy(() -> caseService.getCaseDetail(2L, 1L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 케이스 조회 시 예외 발생")
        void getCaseDetail_CaseNotFound_ThrowsException() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incidentCaseRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> caseService.getCaseDetail(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 사례입니다");
        }

        @Test
        @DisplayName("유효하지 않은 userId로 조회 시 예외 발생")
        void getCaseDetail_InvalidUserId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> caseService.getCaseDetail(null, 1L))
                    .isInstanceOf(AccessDeniedException.class);

            assertThatThrownBy(() -> caseService.getCaseDetail(0L, 1L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}