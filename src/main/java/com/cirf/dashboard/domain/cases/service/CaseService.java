//package com.cirf.dashboard.domain.cases.service;
//
//import com.cirf.dashboard.domain.auth.entity.User;
//import com.cirf.dashboard.domain.auth.repository.UserRepository;  // 기존 auth 도메인의 UserRepository 사용
//import com.cirf.dashboard.domain.cases.dto.request.CaseCreateRequest;
//import com.cirf.dashboard.domain.cases.dto.response.CaseCreateResponse;
//import com.cirf.dashboard.domain.cases.entity.AccountId;
//import com.cirf.dashboard.domain.cases.entity.CaseStatus;
//import com.cirf.dashboard.domain.cases.entity.IncidentCase;
//import com.cirf.dashboard.domain.cases.exception.AccessDeniedException;
//import com.cirf.dashboard.domain.cases.exception.ErrorMessage;
//import com.cirf.dashboard.domain.cases.repository.AccountIdRepository;
//import com.cirf.dashboard.domain.cases.repository.IncidentCaseRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class CaseService {
//
//    private final IncidentCaseRepository incidentCaseRepository;
//    private final AccountIdRepository accountIdRepository;
//    private final UserRepository userRepository;  // auth 도메인의 기존 UserRepository 사용
//
//    @Transactional
//    public CaseCreateResponse createCase(Long userId, CaseCreateRequest req) {
//        // userId 검증 및 User 엔티티 조회
//        if (userId == null || userId <= 0) {
//            log.warn("Invalid userId attempted: {}", userId);
//            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
//        }
//
//        // User 엔티티 조회 (존재하지 않으면 예외 발생)
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> {
//                    log.warn("User not found with id: {}", userId);
//                    return new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
//                });
//
//        // accountIds 존재 여부 확인
//        long exists = accountIdRepository.countExistingByIds(req.getAccountIds());
//        if (exists != req.getAccountIds().size()) {
//            log.warn("Non-existing accountIds detected. Requested: {}, Existing: {}",
//                    req.getAccountIds().size(), exists);
//            throw new IllegalArgumentException("존재하지 않는 accountId가 포함되어 있습니다.");
//        }
//
//        // IncidentCase 엔티티 생성 (정적 팩토리 메서드 사용)
////        IncidentCase entity =  IncidentCase.builder().
////        .req.getCaseName(),
////                req.getCaseDescription(),
////                userId,
////                req.getAccountIds().build
////        );
//
//        List<String> ids = req.getAccountIds(); // List<String>
//        long existing = accountIdRepository.countByAccountIdIn(ids);
//        if (existing != ids.size()) {
//            throw new IllegalArgumentException("존재하지 않는 accountId가 포함되어 있습니다.");
//        }
//
//
//        List<AccountId> accountIds = accountIdRepository.findByAccountId(req.getAccountIds());
//
//        IncidentCase entity = IncidentCase.builder()
//                .user(user)
//                .caseName(req.getCaseName())
//                .description(req.getCaseDescription())
//                .status(CaseStatus.ACTIVE)
//                .accountIdList(accountIds)
//                .build();
//
//        // 엔티티 저장
//        IncidentCase saved = incidentCaseRepository.save(entity);
//
//        // AccountId 엔티티들과 IncidentCase의 관계 설정 (필요한 경우)
//        // updateAccountIdRelations(saved, req.getAccountIds());
//
//        log.info("Case created successfully - caseId: {}, caseName: {}, userId: {}",
//                saved.getId(), saved.getCaseName(), userId);
//
//        return new CaseCreateResponse(saved.getId());
//    }
//}

package com.cirf.dashboard.domain.cases.service;

import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.cases.dto.request.CaseCreateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseCreateResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDeleteResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseListResponse;
import com.cirf.dashboard.domain.cases.dto.request.CaseUpdateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseUpdateResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.stream.Collectors;
import com.cirf.dashboard.domain.cases.entity.AccountId;
import com.cirf.dashboard.domain.cases.entity.CaseStatus;
import com.cirf.dashboard.domain.cases.entity.IncidentCase;
import com.cirf.dashboard.domain.cases.exception.AccessDeniedException;
import com.cirf.dashboard.domain.cases.exception.ErrorMessage;
import com.cirf.dashboard.domain.cases.repository.AccountIdRepository;
import com.cirf.dashboard.domain.cases.repository.IncidentCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cirf.dashboard.domain.cases.entity.IntegrationAccount;
import com.cirf.dashboard.domain.cases.repository.IntegrationAccountRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final IncidentCaseRepository incidentCaseRepository;
    private final AccountIdRepository accountIdRepository;
    private final UserRepository userRepository;
    private final IntegrationAccountRepository integrationAccountRepository;

    /**
     * 사례 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public CaseListResponse getCaseList(Long userId, Integer pageNumber, Integer pageSize) {

        // 1) 사용자 검증
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
                });

        // 2) 페이징 설정
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        // 3) 해당 사용자의 사례 목록 조회
        Page<IncidentCase> casePage = incidentCaseRepository.findByUserId(userId, pageable);

        // 4) DTO 변환
        List<CaseListResponse.CaseItem> content = casePage.getContent().stream()
                .map(incidentCase -> {
                    // 해당 사례에 연결된 accountId 목록 조회
                    List<String> accountIds = accountIdRepository
                            .findByIncidentCaseId(incidentCase.getId())
                            .stream()
                            .map(AccountId::getAccountId)
                            .collect(Collectors.toList());

                    return CaseListResponse.CaseItem.builder()
                            .caseId(incidentCase.getId())
                            .caseName(incidentCase.getCaseName())
                            .caseDescription(incidentCase.getDescription())
                            .status(incidentCase.getStatus().name())
                            .analystName(incidentCase.getUser().getUserName()) // ✅ 수정!
                            .accountIds(accountIds)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Case list retrieved - userId: {}, page: {}, totalElements: {}",
                userId, pageNumber, casePage.getTotalElements());

        // 5) 응답 생성
        return CaseListResponse.builder()
                .content(content)
                .page(casePage.getNumber())
                .size(casePage.getSize())
                .totalElements(casePage.getTotalElements())
                .totalPages(casePage.getTotalPages())
                .build();
    }

    /**
     * 사례 수정 (부분 수정)
     */
    @Transactional
    public CaseUpdateResponse updateCase(Long userId, Long caseId, CaseUpdateRequest req) {

        // 1) userId 검증
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
                });

        // 2) 사례 조회
        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("Case not found with id: {}", caseId);
                    return new IllegalArgumentException("존재하지 않는 사례입니다.");
                });

        // 3) 권한 검증 - 본인이 생성한 사례만 수정 가능
        if (!incidentCase.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to update case {} owned by user {}",
                    userId, caseId, incidentCase.getUser().getId());
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        // 4) 사례 정보 수정 (null이 아닌 값만 수정)
        if (req.getCaseName() != null && !req.getCaseName().isBlank()) {
            incidentCase.updateCaseInfo(req.getCaseName(), incidentCase.getDescription());
        }
        if (req.getCaseDescription() != null && !req.getCaseDescription().isBlank()) {
            incidentCase.updateCaseInfo(incidentCase.getCaseName(), req.getCaseDescription());
        }

        // 5) accountIds 수정 (요청에 포함된 경우에만)
        if (req.getAccountIds() != null && !req.getAccountIds().isEmpty()) {

            // 5-1) 새로운 accountIds 검증
            List<String> newIds = req.getAccountIds();
            long existing = accountIdRepository.countByAccountIdIn(newIds);
            if (existing != newIds.size()) {
                log.warn("Non-existing accountIds detected. Requested: {}, Existing: {}",
                        newIds.size(), existing);
                throw new IllegalArgumentException("존재하지 않는 accountId가 포함되어 있습니다.");
            }

            // 5-2) 기존 연결 해제
            List<AccountId> oldAccounts = accountIdRepository.findByIncidentCaseId(caseId);
            for (AccountId account : oldAccounts) {
                account.setIncidentCase(null);
            }
            accountIdRepository.saveAll(oldAccounts);

            // 5-3) 새로운 연결 설정
            List<AccountId> newAccounts = accountIdRepository.findByAccountIdIn(newIds);
            for (AccountId account : newAccounts) {
                account.setIncidentCase(incidentCase);
            }
            accountIdRepository.saveAll(newAccounts);

            log.info("AccountIds updated for case {} - old count: {}, new count: {}",
                    caseId, oldAccounts.size(), newAccounts.size());
        }

        // 6) 변경사항 저장 (Dirty Checking으로 자동 저장됨)
        log.info("Case updated successfully - caseId: {}, userId: {}", caseId, userId);

        return new CaseUpdateResponse(caseId);
    }


    @Transactional
    public CaseCreateResponse createCase(Long userId, CaseCreateRequest req) {

        // 1) user 검증/조회
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
                });

        // 2) accountIds 검증
        List<String> accountIdStrings = req.getAccountIds();
        if (accountIdStrings == null || accountIdStrings.isEmpty()) {
            throw new IllegalArgumentException("accountIds는 1개 이상이어야 합니다.");
        }

        // 3) IncidentCase 생성/저장 (먼저 저장해야 ID 생김)
        IncidentCase entity = IncidentCase.builder()
                .user(user)
                .caseName(req.getCaseName())
                .description(req.getCaseDescription())
                .status(CaseStatus.ACTIVE)
                .build();
        IncidentCase saved = incidentCaseRepository.save(entity);

        log.info("IncidentCase created - caseId: {}, caseName: {}", saved.getId(), saved.getCaseName());

        // 4) AccountId 엔티티 생성 및 저장 ✅ 수정된 부분!
        List<AccountId> accountEntities = accountIdStrings.stream()
                .map(accountIdString -> {
                    // 기존 AccountId 엔티티가 있는지 확인
                    AccountId accountEntity = accountIdRepository
                            .findByAccountId(accountIdString)
                            .orElseGet(() -> {
                                // 없으면 새로 생성
                                AccountId newAccount = AccountId.builder()
                                        .accountId(accountIdString)
                                        .roleArn("arn:aws:iam::account:role/DefaultRole")  // 기본값
                                        .roleCheck(false)
                                        .incidentCase(saved)  // 사례와 연결
                                        .build();

                                log.info("Creating new AccountId: {}", accountIdString);
                                return newAccount;
                            });

                    // 이미 존재하는 경우 사례와 연결
                    if (accountEntity.getId() != null) {
                        accountEntity.setIncidentCase(saved);
                        log.info("Linking existing AccountId to case: {}", accountIdString);
                    }

                    return accountEntity;
                })
                .toList();

        // 모두 저장
        accountIdRepository.saveAll(accountEntities);

        log.info("Case created successfully - caseId: {}, caseName: {}, userId: {}, AccountIds: {}",
                saved.getId(), saved.getCaseName(), userId, accountEntities.size());

        // 5) IntegrationAccount 생성 및 저장 (DynamoDB)
        List<IntegrationAccount> accounts = req.getAccountIds().stream()
                .map(accountIdString -> IntegrationAccount.builder()
                        .pk("USER#%d#CASE#%d".formatted(userId, saved.getId()))
                        .sk("ACCOUNT#%s".formatted(accountIdString))
                        .roleArn("arn:aws:iam::%s:role/IRAutomationRole".formatted(accountIdString))
                        .roleCheck(false)
                        .accountId(accountIdString)
                        .userId(userId)
                        .caseId(saved.getId())
                        .build()
                )
                .toList();

        integrationAccountRepository.saveAll(accounts);

        log.info("IntegrationAccounts created - count: {}", accounts.size());

        return new CaseCreateResponse(saved.getId());
    }

    @Transactional
    public CaseDeleteResponse deleteCase(Long userId, Long caseId) {

        // 1) userId 검증
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
                });

        // 2) 사례 조회
        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("Case not found with id: {}", caseId);
                    return new IllegalArgumentException("존재하지 않는 사례입니다.");
                });

        // 3) 권한 검증 - 본인이 생성한 사례만 삭제 가능
        if (!incidentCase.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to delete case {} owned by user {}",
                    userId, caseId, incidentCase.getUser().getId());
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        // 4) 연관된 AccountId들의 참조 해제
        List<AccountId> relatedAccounts = accountIdRepository.findByIncidentCaseId(caseId);
        for (AccountId account : relatedAccounts) {
            account.setIncidentCase(null);
        }
        accountIdRepository.saveAll(relatedAccounts);

        // 5) 사례 삭제
        incidentCaseRepository.delete(incidentCase);

        log.info("Case deleted successfully - caseId: {}, userId: {}", caseId, userId);

        return new CaseDeleteResponse(caseId);
    }

    /**
     * 단일 사례 상세 조회
     */
    @Transactional(readOnly = true)
    public CaseDetailResponse getCaseDetail(Long userId, Long caseId) {

        // 1) 사용자 검증
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
                });

        // 2) 사례 조회
        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("Case not found with id: {}", caseId);
                    return new IllegalArgumentException("존재하지 않는 사례입니다.");
                });

        // 3) 권한 검증 - 본인이 생성한 사례만 조회 가능
        if (!incidentCase.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to view case {} owned by user {}",
                    userId, caseId, incidentCase.getUser().getId());
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        // 4) 해당 사례에 연결된 accountId 목록 조회 ✅
        List<String> accountIds = accountIdRepository
                .findByIncidentCaseId(caseId)
                .stream()
                .map(AccountId::getAccountId)
                .collect(Collectors.toList());

        // 5) DTO 변환 및 응답
        log.info("Case detail retrieved successfully - caseId: {}, userId: {}", caseId, userId);

        return CaseDetailResponse.builder()
                .caseId(incidentCase.getId())
                .caseName(incidentCase.getCaseName())
                .caseDescription(incidentCase.getDescription())
                .status(incidentCase.getStatus().name().toLowerCase()) // ACTIVE → "active"
                .analystName(incidentCase.getUser().getUserName()) // ✅ 추가! (또는 getLoginId())
                .accountIds(accountIds) // ✅ 추가!
                .build();
    }




} // 클래스 닫는 중괄호 - 이 위치 확인!