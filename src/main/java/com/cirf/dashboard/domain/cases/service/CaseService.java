package com.cirf.dashboard.domain.cases.service;

import com.cirf.dashboard.domain.auth.entity.Tenant;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.cases.dto.request.CaseCreateRequest;
import com.cirf.dashboard.domain.cases.dto.response.*;
import com.cirf.dashboard.domain.cases.dto.request.CaseUpdateRequest;
import com.cirf.dashboard.domain.cases.entity.*;
import com.cirf.dashboard.domain.cases.exception.*;
import com.cirf.dashboard.domain.cases.repository.CaseBucketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import com.cirf.dashboard.domain.cases.entity.DeploymentStatus;
import com.cirf.dashboard.domain.cases.repository.DeploymentStatusRepository;

import com.cirf.dashboard.domain.cases.repository.AccountIdRepository;
import com.cirf.dashboard.domain.cases.repository.IncidentCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cirf.dashboard.domain.cases.repository.IntegrationAccountRepository;

import java.io.IOException;
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
    private final CloudFormationStackService cloudFormationStackService;
    private final DeploymentSseHub deploymentSseHub;

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

        initializeDeploymentStatus(caseId, accountId);

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
    public void initializeDeploymentStatus(Long caseId, String accountId) {

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

        // 3. 배포 상태 조회 (가장 최근 레코드)
        DeploymentStatus deploymentStatus = deploymentStatusRepository
                .findFirstByCaseIdAndAccountIdOrderByCreatedAtDesc(caseId, accountId)
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

        // 가장 최근 생성된 DeploymentStatus 조회
        DeploymentStatus deploymentStatus = deploymentStatusRepository
                .findFirstByCaseIdAndAccountIdOrderByCreatedAtDesc(caseId, accountId)
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

    // CaseService.java에 추가
    @Transactional(readOnly = true)
    public DeploymentStatusResponse getDeploymentStatusOrDefault(Long caseId, String accountId) {
        return deploymentStatusRepository
                .findFirstByCaseIdAndAccountIdOrderByCreatedAtDesc(caseId, accountId)
                .map(DeploymentStatusResponse::from)
                .orElseThrow();
    }

    @Transactional
    public void updateDeploymentStatusAndPublish(Long caseId, String accountId, String status, String statusReason) {

        DeploymentStatus.StackStatus stackStatus = DeploymentStatus.StackStatus.valueOf(status);

        log.info("Updating deployment status - caseId: {}, accountId: {}, status: {}",
                caseId, accountId, status);

        // 가장 최근 생성된 DeploymentStatus 조회
        DeploymentStatus deploymentStatus = deploymentStatusRepository
                .findFirstByCaseIdAndAccountIdOrderByCreatedAtDesc(caseId, accountId)
                .orElseThrow(() -> new IllegalStateException("배포 상태 정보가 없습니다."));

        deploymentStatus.updateStatus(stackStatus, statusReason);
        deploymentStatusRepository.save(deploymentStatus);

        log.info("Deployment status updated successfully");
        DeploymentStatusResponse response = getDeploymentStatus(caseId, accountId); // 최신 상태

        deploymentSseHub.publish(caseId, accountId, response);

        if (stackStatus == DeploymentStatus.StackStatus.DEPLOYED
                || stackStatus == DeploymentStatus.StackStatus.FAILED) {
            deploymentSseHub.complete(caseId, accountId, "ok");
        }
    }

    /**
     * 배포 상태 실시간 스트리밍 (SSE)
     */
//    public SseEmitter streamDeploymentStatus(Long userId, Long caseId, String accountId) {
//
//        validateUser(userId);
//        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
//                .orElseThrow(() -> new NotFoundBucketException(ErrorMessage.CASE_NOT_FOUND));
//
//        if (!incidentCase.getUser().getId().equals(userId)) {
//            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
//        }
//
//        boolean accountExists = incidentCase.getAccountIdList().stream()
//                .anyMatch(acc -> acc.getAccountId().equals(accountId));
//        if (!accountExists) throw new NotFoundAccountException(ErrorMessage.ACCOUNT_NOT_FOUND);
//
//        SseEmitter emitter = new SseEmitter(300_000L);
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        AtomicBoolean stopped = new AtomicBoolean(false);
//
//        Runnable stop = () -> {
//            stopped.set(true);
//            executor.shutdownNow();
//        };
//
//        emitter.onCompletion(stop);
//        emitter.onTimeout(() -> {
//            log.warn("SSE timeout - caseId={}, accountId={}", caseId, accountId);
//            emitter.complete();
//            stop.run();
//        });
//        emitter.onError(e -> {
//            log.warn("SSE error - caseId={}, accountId={}, err={}", caseId, accountId, e.toString());
//            stop.run();
//        });
//
//        executor.execute(() -> {
//            try {
//                // 연결 직후 한번 보내주면 프록시/브라우저 안정성 올라감
//                try {
//                    emitter.send(SseEmitter.event().name("connected").data("ok"));
//                } catch (IOException ioe) {
//                    return; // 이미 클라가 끊김
//                }
//
//                while (!stopped.get()) {
//                    Optional<DeploymentStatus> opt = deploymentStatusRepository
//                            .findByCaseIdAndAccountId(caseId, accountId);
//
//                    if (opt.isEmpty()) {
//                        // 아직 생성 전이면 에러로 끊지 말고 상태만 보내기
//                        try {
//                            emitter.send(SseEmitter.event()
//                                    .name("deployment-status")
//                                    .data(Map.of("stackStatus", "NOT_READY_YET")));
//                        } catch (IOException ioe) {
//                            break;
//                        }
//                    } else {
//                        DeploymentStatus ds = opt.get();
//                        DeploymentStatusResponse response = DeploymentStatusResponse.from(ds);
//
//                        try {
//                            emitter.send(SseEmitter.event().name("deployment-status").data(response));
//                        } catch (IOException ioe) {
//                            // 클라이언트/프록시가 끊은 것 → 정상 종료
//                            break;
//                        }
//
//                        if (ds.getStackStatus() == DeploymentStatus.StackStatus.DEPLOYED
//                                || ds.getStackStatus() == DeploymentStatus.StackStatus.FAILED) {
//                            emitter.complete();
//                            break;
//                        }
//                    }
//
//                    // heartbeat는 2초 폴링이면 사실상 계속 데이터가 나가서 필요성이 낮지만,
//                    // DB가 변동 없을 때도 프록시 끊김이 있다면 5~10초마다 heartbeat 추천
//                    Thread.sleep(2000);
//                }
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                emitter.complete();
//            } catch (Exception e) {
//                // 진짜 서버 내부 오류만 error로
//                log.error("SSE internal error - caseId={}, accountId={}", caseId, accountId, e);
//                emitter.completeWithError(e);
//            } finally {
//                stop.run();
//            }
//        });
//
//        return emitter;
//    }


    // CaseService.java에 추가

    public void validateCaseOwnership(Long userId, Long caseId, String accountId) {
        validateUser(userId);

        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
                .orElseThrow(() -> new NotFoundBucketException(ErrorMessage.CASE_NOT_FOUND));

        if (!incidentCase.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }

        boolean accountExists = incidentCase.getAccountIdList().stream()
                .anyMatch(acc -> acc.getAccountId().equals(accountId));

        if (!accountExists) throw new NotFoundAccountException(ErrorMessage.ACCOUNT_NOT_FOUND);
    }

    /**
     * Stack 생성 정보 실시간 스트리밍 (SSE)
     */
//    public SseEmitter streamStackInfo(Long userId, Long caseId, String accountId) {
//        log.info("Starting stack info streaming - caseId: {}, accountId: {}, userId: {}",
//                caseId, accountId, userId);
//
//        validateUser(userId);
//
//        IncidentCase incidentCase = incidentCaseRepository.findById(caseId)
//                .orElseThrow(() -> new NotFoundBucketException(ErrorMessage.CASE_NOT_FOUND));
//
//        if (!incidentCase.getUser().getId().equals(userId)) {
//            log.warn("User {} attempted to view case {} owned by user {}",
//                    userId, caseId, incidentCase.getUser().getId());
//            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
//        }
//
//        boolean accountExists = incidentCase.getAccountIdList().stream()
//                .anyMatch(acc -> acc.getAccountId().equals(accountId));
//        if (!accountExists) throw new NotFoundAccountException(ErrorMessage.ACCOUNT_NOT_FOUND);
//
//        // 5분 timeout
//        SseEmitter emitter = new SseEmitter(300_000L);
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//
//        AtomicBoolean stopped = new AtomicBoolean(false);
//
//        Runnable stop = () -> {
//            stopped.set(true);
//            executor.shutdownNow();
//        };
//
//        // 콜백들: 종료 플래그 + complete
//        emitter.onCompletion(() -> {
//            log.info("SSE connection completed - caseId: {}, accountId: {}", caseId, accountId);
//            stop.run();
//        });
//        emitter.onTimeout(() -> {
//            log.warn("SSE connection timeout - caseId: {}, accountId: {}", caseId, accountId);
//            try { emitter.complete(); } catch (Exception ignored) {}
//            stop.run();
//        });
//        emitter.onError(e -> {
//            log.warn("SSE connection error - caseId: {}, accountId: {}, err={}",
//                    caseId, accountId, e.toString());
//            stop.run();
//        });
//
//        final int MAX_NOT_CREATED_POLLS = 150; // 5분 (2초 폴링 기준)
//        final int HEARTBEAT_EVERY = 5;         // 2초 폴링 기준 10초마다 heartbeat 권장
//
//        executor.execute(() -> {
//            int pollCount = 0;
//            int notCreatedCount = 0;
//
//            try {
//                if (!safeSend(emitter, SseEmitter.event().name("connected").data("ok"))) {
//                    return; // 이미 클라이언트가 끊김
//                }
//
//                while (!stopped.get()) {
//                    pollCount++;
//
//                    try {
//                        StackInfoResponse stackInfo = cloudFormationStackService.getStackInfo(caseId, accountId);
//                        notCreatedCount = 0;
//
//                        String status = stackInfo.getStackStatus();
//                        log.debug("Fetched stack info - caseId: {}, accountId: {}, status: {}, poll: {}",
//                                caseId, accountId, status, pollCount);
//
//                        if (isTerminal(status)) {
//                            if (!safeSend(emitter, SseEmitter.event()
//                                    .name("complete")
//                                    .data(Map.of(
//                                            "message", "Stack deployment completed",
//                                            "finalStatus", status,
//                                            "caseId", caseId,
//                                            "accountId", accountId
//                                    )))) {
//                                return;
//                            }
//                            emitter.complete();
//                            break;
//                        }
//
//                        if (!safeSend(emitter, SseEmitter.event().name("stack-info").data(stackInfo))) {
//                            return;
//                        }
//
//                    } catch (IllegalStateException e) {
//                        notCreatedCount++;
//
//                        NotCreatedResponse notCreatedResponse = NotCreatedResponse.builder()
//                                .caseId(caseId)
//                                .accountId(accountId)
//                                .stackName(String.format("CIRF-Case-%d-Account-%s", caseId, accountId))
//                                .stackStatus("NOT_CREATED")
//                                .statusReason(String.format(
//                                        "Stack이 아직 생성되지 않았습니다. CloudFormation Launch URL을 통해 배포를 시작하세요. (%d/%d)",
//                                        notCreatedCount, MAX_NOT_CREATED_POLLS))
//                                .build();
//
//                        if (!safeSend(emitter, SseEmitter.event().name("stack-info").data(notCreatedResponse))) {
//                            return;
//                        }
//
//                        if (notCreatedCount >= MAX_NOT_CREATED_POLLS) {
//                            safeSend(emitter, SseEmitter.event()
//                                    .name("complete")
//                                    .data(Map.of(
//                                            "message", "Stack not created after timeout",
//                                            "finalStatus", "NOT_CREATED",
//                                            "caseId", caseId,
//                                            "accountId", accountId
//                                    )));
//                            emitter.complete();
//                            break;
//                        }
//
//                    } catch (RuntimeException e) {
//                        String msg = (e.getMessage() == null) ? "" : e.getMessage();
//                        boolean fatal = looksFatalAuthOrAccess(msg);
//
//                        log.warn("Stack access/runtime error - fatal={}, caseId={}, accountId={}, poll={}, msg={}",
//                                fatal, caseId, accountId, pollCount, msg);
//
//                        if (fatal) {
//                            // 회복 불가능: error 이벤트 보내고 종료(여긴 completeWithError 유지 가능)
//                            safeSend(emitter, SseEmitter.event()
//                                    .name("error")
//                                    .data(Map.of(
//                                            "message", "Stack 접근 실패: " + msg,
//                                            "errorType", "ACCESS_DENIED",
//                                            "caseId", caseId,
//                                            "accountId", accountId
//                                    )));
//                            emitter.completeWithError(e);
//                            break;
//                        } else {
//                            // 회복 가능(일시적): 진행중처럼 보내고 계속 폴링
//                            if (!safeSend(emitter, SseEmitter.event()
//                                    .name("stack-info")
//                                    .data(Map.of(
//                                            "caseId", caseId,
//                                            "accountId", accountId,
//                                            "stackName", String.format("CIRF-Case-%d-Account-%s", caseId, accountId),
//                                            "stackStatus", "CREATE_IN_PROGRESS",
//                                            "statusReason", "Stack 생성/권한 준비 중... (시도 " + pollCount + ")"
//                                    )))) {
//                                return;
//                            }
//                        }
//                    }
//
//                    if (pollCount % HEARTBEAT_EVERY == 0) {
//                        if (!safeSend(emitter, SseEmitter.event()
//                                .name("heartbeat")
//                                .data(Map.of("ts", System.currentTimeMillis())))) {
//                            return;
//                        }
//                    }
//
//                    Thread.sleep(2000);
//                }
//
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                try { emitter.complete(); } catch (Exception ignored) {}
//            } catch (Exception e) {
//                // 진짜 서버 내부 오류만 여기로
//                log.error("Error during stack info streaming - caseId: {}, accountId: {}", caseId, accountId, e);
//                emitter.completeWithError(e);
//            } finally {
//                stop.run();
//                log.info("SSE stream ended - caseId: {}, accountId: {}, totalPolls: {}", caseId, accountId, pollCount);
//            }
//        });
//
//        return emitter;
//    }

//    private boolean safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
//        try {
//            emitter.send(event);
//            return true;
//        } catch (IOException ioe) {
//            // Broken pipe / reset by peer 등: 클라이언트가 끊은 것
//            return false;
//        }
//    }
//
//    private boolean isTerminal(String status) {
//        if (status == null) return false;
//        return "CREATE_COMPLETE".equals(status)
//                || "UPDATE_COMPLETE".equals(status)
//                || "DELETE_COMPLETE".equals(status)
//                || "ROLLBACK_COMPLETE".equals(status)
//                || "CREATE_FAILED".equals(status)
//                || status.endsWith("_FAILED");
//    }
//
//    private boolean looksFatalAuthOrAccess(String msg) {
//        String m = msg.toLowerCase();
//        return m.contains("not authorized")
//                || m.contains("accessdenied")
//                || m.contains("is not authorized")
//                || m.contains("forbidden")
//                || m.contains("describeStacks".toLowerCase())
//                || m.contains("no identity-based policy")
//                || m.contains("assumerole")
//                || m.contains("irautomationrole");
//    }


    public void validateUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new AccessDeniedException(ErrorMessage.ACCESS_DENIED);
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }

}