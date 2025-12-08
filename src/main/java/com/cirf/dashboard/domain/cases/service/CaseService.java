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
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import com.cirf.dashboard.domain.cases.dto.response.OnboardingInfoResponse;
// 2. Import 추가
import com.cirf.dashboard.domain.cases.entity.DeploymentStatus;
import com.cirf.dashboard.domain.cases.dto.response.DeploymentStatusResponse;
import com.cirf.dashboard.domain.cases.repository.DeploymentStatusRepository;

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
import com.cirf.dashboard.domain.cases.dto.response.StackInfoResponse;

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
    private final CloudFormationTemplateService cloudFormationTemplateService;
    private final EmailService emailService;
    private final CloudFormationStackService cloudFormationStackService;  // 의존성 주입 추가

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

        // 6) CloudFormation 템플릿 생성 및 S3 업로드 (각 accountId별로)
        List<String> templateUrls = new ArrayList<>();
        if (bucketName != null) {
            for (String accountId : accountIdStrings) {
                try {
                    String templateUrl = cloudFormationTemplateService.createAndUploadTemplate(
                            saved.getId(),
                            accountId,
                            bucketName
                    );
                    templateUrls.add(templateUrl);
                    log.info("CloudFormation template created for case {} and account {}: {}",
                            saved.getId(), accountId, templateUrl);
                } catch (Exception e) {
                    log.error("Failed to create CloudFormation template for case {} and account {}: {}",
                            saved.getId(), accountId, e.getMessage());
                }
            }
        } else {
            log.warn("Skipping CloudFormation template creation - S3 bucket was not created for case {}",
                    saved.getId());
        }

        log.info("Case created successfully - caseId: {}, caseName: {}, userId: {}, AccountIds: {}, CloudFormation templates: {}",
                saved.getId(), saved.getCaseName(), userId, accountIdStrings.size(), templateUrls.size());

        // 7) DynamoDB에 저장 (사례 정보 및 생성된 버킷명)
        createIntegrationAccount(user, req.getAccountIds(), saved);
        createCaseBucket(userId, saved.getId(), bucketName);
        initializeDeploymentStatus(saved.getId(), req.getAccountIds());

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

    /**
     * 온보딩 정보 조회
     * - CloudFormation 템플릿 내용
     * - S3 Presigned URL
     * - CloudFormation 런치 링크
     */
    public OnboardingInfoResponse getOnboardingInfo(Long caseId, String accountId) {
        log.info("Fetching onboarding info for caseId: {}, accountId: {}", caseId, accountId);

        // 1. 사례 존재 여부 확인
        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사례입니다: " + caseId));

        // 2. 계정 ID 확인
        boolean accountExists = incidentCase.getAccountIdList().stream()
                .anyMatch(acc -> acc.getAccountId().equals(accountId));

        if (!accountExists) {
            throw new IllegalArgumentException("해당 사례에 등록되지 않은 계정입니다: " + accountId);
        }

        // 3. S3 버킷명 조회 (DynamoDB에서 실제 값 가져오기!) ✅
        CaseBucket bucket = caseBucketRepository.findCaseBucketByUserIdAndCaseId(
                incidentCase.getUser().getId(), caseId);

        if (bucket == null) {
            throw new IllegalArgumentException("사례에 연결된 S3 버킷이 없습니다: " + caseId);
        }

        String bucketName = bucket.getBucketName();  // ✅ 실제 버킷명 사용!
        log.info("Found bucket for case {}: {}", caseId, bucketName);

        // 4. S3 키 생성
        String s3Key = String.format("cloudformation-templates/case-%d/account-%s/template.yml",
                caseId, accountId);

        // 5. Presigned URL 생성 (7일 유효)
        int expirationMinutes = 60 * 24 * 7; // 7일
        String presignedUrl = cloudFormationTemplateService.generatePresignedUrl(
                bucketName, s3Key, expirationMinutes
        );

        // 6. CloudFormation 런치 링크 생성
        String launchUrl = cloudFormationTemplateService.generateCloudFormationLaunchUrl(
                presignedUrl, caseId, accountId
        );

        // 7. 템플릿 내용 가져오기
        String templateContent = cloudFormationTemplateService.getTemplateContent(caseId, accountId);

        return OnboardingInfoResponse.builder()
                .templateContent(templateContent)
                .presignedUrl(presignedUrl)
                .launchUrl(launchUrl)
                .accountId(accountId)
                .caseId(caseId)
                .expirationMinutes(expirationMinutes)
                .build();
    }

    // CaseService.java에 추가할 메서드


    /**
     * 온보딩 정보를 이메일로 전송
     */
    public void sendOnboardingEmail(Long caseId, String accountId, List<String> emails) {
        log.info("Sending onboarding email for caseId: {}, accountId: {}, to: {}",
                caseId, accountId, emails);

        // 온보딩 정보 조회
        OnboardingInfoResponse onboardingInfo = getOnboardingInfo(caseId, accountId);

        // 이메일 전송
        emailService.sendOnboardingEmail(
                emails,
                caseId,
                accountId,
                onboardingInfo.getLaunchUrl(),
                onboardingInfo.getPresignedUrl(),
                onboardingInfo.getTemplateContent()
        );

        log.info("Onboarding email sent successfully to {}", emails.size());
    }


    /**
     * CloudFormation Stack 정보 조회
     */
    public StackInfoResponse getStackInfo(Long caseId, String accountId) {
        log.info("Getting stack info for caseId: {}, accountId: {}", caseId, accountId);

        // 1. 사례 존재 여부 확인
        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사례입니다: " + caseId));

        // 2. 계정 ID 확인
        boolean accountExists = incidentCase.getAccountIdList().stream()
                .anyMatch(acc -> acc.getAccountId().equals(accountId));

        if (!accountExists) {
            throw new IllegalArgumentException("해당 사례에 등록되지 않은 계정입니다: " + accountId);
        }

        // 3. CloudFormation Stack 정보 조회
        return cloudFormationStackService.getStackInfo(caseId, accountId);
    }

    // ============================================
// CaseService.java에 추가할 내용
// ============================================

    // 1. 의존성 주입 추가 (필드)
    private final DeploymentStatusRepository deploymentStatusRepository;


// 3. 메서드들 추가

    /**
     * 사례 생성 시 배포 상태 초기화
     * createCase() 메서드 내부에서 호출
     */
    private void initializeDeploymentStatus(Long caseId, List<String> accountIds) {
        for (String accountId : accountIds) {
            String stackName = String.format("CIRF-Case-%d-Account-%s", caseId, accountId);
            String roleArn = String.format("arn:aws:iam::%s:role/IRAutomationRole", accountId);

            DeploymentStatus deploymentStatus = DeploymentStatus.builder()
                    .caseId(caseId)
                    .accountId(accountId)
                    .stackName(stackName)
                    .stackStatus(DeploymentStatus.StackStatus.NOT_DEPLOYED)
                    .statusReason("CloudFormation 템플릿이 생성되었습니다. launchUrl을 통해 배포를 시작하세요.")
                    .roleArn(roleArn)
                    .build();

            deploymentStatusRepository.save(deploymentStatus);
            log.info("Deployment status initialized for case {} and account {}", caseId, accountId);
        }
    }

    /**
     * 배포 상태 조회
     */
    public DeploymentStatusResponse getDeploymentStatus(Long caseId, String accountId) {
        log.info("Getting deployment status for caseId: {}, accountId: {}", caseId, accountId);

        // 1. 사례 존재 여부 확인
        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사례입니다: " + caseId));

        // 2. 계정 ID 확인
        boolean accountExists = incidentCase.getAccountIdList().stream()
                .anyMatch(acc -> acc.getAccountId().equals(accountId));

        if (!accountExists) {
            throw new IllegalArgumentException("해당 사례에 등록되지 않은 계정입니다: " + accountId);
        }

        // 3. 배포 상태 조회
        DeploymentStatus deploymentStatus = deploymentStatusRepository
                .findByCaseIdAndAccountId(caseId, accountId)
                .orElseThrow(() -> new IllegalStateException("배포 상태 정보가 없습니다."));

        return DeploymentStatusResponse.from(deploymentStatus);
    }

    /**
     * 배포 상태 업데이트 (Webhook 또는 수동 호출용)
     */
    @Transactional
    public void updateDeploymentStatus(Long caseId, String accountId,
                                       DeploymentStatus.StackStatus status, String statusReason) {
        log.info("Updating deployment status - caseId: {}, accountId: {}, status: {}",
                caseId, accountId, status);

        DeploymentStatus deploymentStatus = deploymentStatusRepository
                .findByCaseIdAndAccountId(caseId, accountId)
                .orElseThrow(() -> new IllegalStateException("배포 상태 정보가 없습니다."));

        deploymentStatus.updateStatus(status, statusReason);
        deploymentStatusRepository.save(deploymentStatus);

        log.info("Deployment status updated successfully");
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