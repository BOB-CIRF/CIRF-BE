package com.cirf.dashboard.domain.cases.service;

import com.cirf.dashboard.domain.auth.entity.Tenant;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.cases.dto.request.CaseCreateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseCreateResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDeleteResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseListResponse;
import com.cirf.dashboard.domain.cases.dto.request.CaseUpdateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseUpdateResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDetailResponse;
import com.cirf.dashboard.domain.cases.entity.*;
import com.cirf.dashboard.domain.cases.exception.ExistsAccountIdException;
import com.cirf.dashboard.domain.cases.exception.NotFoundBucketException;
import com.cirf.dashboard.domain.cases.repository.CaseBucketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.stream.Collectors;

import com.cirf.dashboard.domain.cases.exception.AccessDeniedException;
import com.cirf.dashboard.domain.cases.exception.ErrorMessage;
import com.cirf.dashboard.domain.cases.repository.AccountIdRepository;
import com.cirf.dashboard.domain.cases.repository.IncidentCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final CaseBucketRepository caseBucketRepository;

    private final S3ConfigService s3ConfigService;
    private final S3EventService s3EventService;

    @Value("${aws.sqs.shared-queue-arn}")
    private String sharedQueueArn;

    @Value("${aws.sqs.shared-queue-url}")
    private String sharedQueueUrl;


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
                            .analystName(incidentCase.getUser().getUserName())
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


    @Transactional
    public CaseUpdateResponse updateCase(Long userId, Long caseId, CaseUpdateRequest req) {

        // 1) userId 검증
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId).orElseThrow(()-> new AccessDeniedException(ErrorMessage.ACCESS_DENIED));

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
            incidentCase.getAccountIdList().clear();

            for (String newAccount : req.getAccountIds()) {
                Optional<AccountId> accountEntity = accountIdRepository.findByAccountId(newAccount);

                if (accountEntity.isPresent() && !accountEntity.get().getIncidentCase().getId().equals(incidentCase.getId())) {
                    throw new ExistsAccountIdException(ErrorMessage.EXISTS_ACCOUNT_ID);
                }

                AccountId account = AccountId.builder()
                        .incidentCase(incidentCase)
                        .accountId(newAccount)
                        .roleArn("arn:aws:iam::%s:role/IRAutomationRole".formatted(newAccount))
                        .roleCheck(true)
                        .build();

                incidentCase.getAccountIdList().add(account);
            }

            integrationAccountRepository.deleteByUserIdAndCaseId(userId, caseId);
            createIntegrationAccount(user, req.getAccountIds(), incidentCase);

        }

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

        Tenant tenant = user.getTenant();

        // 2) accountIds 검증
        List<String> accountIdStrings = req.getAccountIds();
        if (accountIdStrings == null || accountIdStrings.isEmpty()) {
            throw new IllegalArgumentException("accountIds는 1개 이상이어야 합니다.");
        }

        for (String accountIdString : accountIdStrings) {
            Optional<AccountId> accountEntity = accountIdRepository.findByAccountId(accountIdString);

            if (accountEntity.isPresent()) {
                throw new ExistsAccountIdException(ErrorMessage.EXISTS_ACCOUNT_ID);
            }
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

        accountIdStrings.forEach(accountIdString -> {
            AccountId account = AccountId.builder()
                    .accountId(accountIdString)
                    .incidentCase(saved)
                    .roleArn("arn:aws:iam::%s:role/IRAutomationRole".formatted(accountIdString))
                    .roleCheck(true)
                    .build();
            saved.getAccountIdList().add(account);
        });

        // 4) S3 버킷 생성
        String bucketName = null;
        try {
            bucketName = s3EventService.createCaseBucket(tenant.getId(), saved.getId());
            log.info("S3 bucket created for case {} : {}", saved.getId(), bucketName);
        } catch (Exception e) {
            log.error("Failed to create S3 bucket for case {}: {}", saved.getId(), e.getMessage());
        }

        // 5) S3 → 공유 SQS 이벤트 알림 설정
        if (bucketName != null) {
            try {
                s3ConfigService.setupS3ToSqsNotification(bucketName, sharedQueueArn, sharedQueueUrl);
                log.info("S3 to shared SQS notification configured for case {} -> {}", saved.getId(), sharedQueueArn);
            } catch (Exception e) {
                log.error("Failed to setup S3-SQS notification for case {}: {}", saved.getId(), e.getMessage());
            }
        }

        log.info("Case created successfully - caseId: {}, caseName: {}, userId: {}, AccountIds: {}",
                saved.getId(), saved.getCaseName(), userId, accountIdStrings.size());

        // DynamoDB에 저장 (사례 정보 및 생성된 버킷명)
        createIntegrationAccount(user, req.getAccountIds(), saved);
        createCaseBucket(userId, saved.getId(), bucketName);

        return new CaseCreateResponse(saved.getId());
    }

    private void createIntegrationAccount(User user, List<String> accountIds, IncidentCase saved){
        List<IntegrationAccount> accounts = accountIds.stream()
                .map(accountIdString -> IntegrationAccount.builder()
                        .pk("USER#%d#CASE#%d".formatted(user.getId(), saved.getId()))
                        .sk("ACCOUNT#%s".formatted(accountIdString))
                        .roleArn("arn:aws:iam::%s:role/IRAutomationRole".formatted(accountIdString))
                        .roleCheck(true)
                        .accountId(accountIdString)
                        .userId(user.getId())
                        .caseId(saved.getId())
                        .tenantId(user.getTenant().getId())
                        .build()
                )
                .toList();

        integrationAccountRepository.saveAll(accounts);

        log.info("IntegrationAccounts created - count: {}", accounts.size());
    }

    private void createCaseBucket(long userId, long caseId, String bucketName) {
        CaseBucket caseBucket = CaseBucket.builder()
                .pk("BUCKET#USER#%d#CASE#%d".formatted(userId, caseId))
                .sk("METADATA")
                .userId(userId)
                .caseId(caseId)
                .bucketName(bucketName)
                .build();

        caseBucketRepository.save(caseBucket);
    }

    @Transactional
    public CaseDeleteResponse deleteCase(Long userId, Long caseId) {

        // 1) userId 검증
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        validateUser(userId);

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

        // 4) 사례 삭제
        incidentCaseRepository.delete(incidentCase);

        integrationAccountRepository.deleteByUserIdAndCaseId(userId, caseId);

        CaseBucket bucket = caseBucketRepository.findCaseBucketByUserIdAndCaseId(userId, caseId);

        if (bucket == null) {
            throw new NotFoundBucketException(ErrorMessage.BUCKET_NOT_FOUND);
        }

        s3EventService.deleteBucketWithContents(bucket.getBucketName());

        return new CaseDeleteResponse(caseId);
    }


    @Transactional(readOnly = true)
    public CaseDetailResponse getCaseDetail(Long userId, Long caseId) {

        // 1) 사용자 검증
        if (userId == null || userId <= 0) {
            log.warn("Invalid userId attempted: {}", userId);
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        validateUser(userId);

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
                .analystName(incidentCase.getUser().getUserName())
                .accountIds(accountIds)
                .build();
    }

    public void validateUser(long userId) {
        // userId 검증
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }

}