package com.cirf.dashboard.domain.cases.controller;

import com.cirf.dashboard.domain.cases.controller.ResponseMessage;
import com.cirf.dashboard.domain.cases.dto.request.CaseCreateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseCreateResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDeleteResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseListResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDetailResponse;
import com.cirf.dashboard.domain.cases.service.CaseService;
import com.cirf.dashboard.domain.cases.dto.request.SendOnboardingEmailRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.cirf.dashboard.domain.cases.dto.request.CaseUpdateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseUpdateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cirf.dashboard.domain.cases.dto.response.OnboardingInfoResponse;
import com.cirf.dashboard.domain.cases.dto.response.StackInfoResponse;
import com.cirf.dashboard.domain.cases.dto.response.DeploymentStatusResponse;
import com.cirf.dashboard.domain.cases.entity.DeploymentStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@Slf4j // 추가!
@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    /**
     * 사례 생성
     * POST /api/v1/cases
     */

    @PostMapping
    public ResponseEntity<ResponseMessage<CaseCreateResponse>> createCase(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody CaseCreateRequest request
    ) {
        CaseCreateResponse response = caseService.createCase(userId, request);

        return ResponseEntity.ok(
                ResponseMessage.<CaseCreateResponse>builder()
                        .status(200)
                        .message("사례 생성을 완료했습니다.")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{caseId}")
    public ResponseEntity<ResponseMessage<CaseDeleteResponse>> deleteCase(
            @RequestHeader("userId") Long userId,
            @PathVariable("caseId") Long caseId) {

        log.info("Delete case request - userId: {}, caseId: {}", userId, caseId);

        CaseDeleteResponse response = caseService.deleteCase(userId, caseId);

        return ResponseEntity.ok(
                ResponseMessage.<CaseDeleteResponse>builder()
                        .status(200)
                        .message("사례가 성공적으로 삭제되었습니다.")
                        .data(response)
                        .build()
        );
    }

    /**
     * 사례 목록 조회 (페이징)
     * GET /api/v1/cases?pageNumber=0&pageSize=20
     */
    @GetMapping
    public ResponseEntity<ResponseMessage<CaseListResponse>> getCaseList(
            @RequestHeader("userId") Long userId,
            @RequestParam(value = "pageNumber", defaultValue = "0") Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {

        log.info("Get case list request - userId: {}, pageNumber: {}, pageSize: {}",
                userId, pageNumber, pageSize);

        // pageSize 유효성 검증 (1~200)
        if (pageSize < 1 || pageSize > 200) {
            pageSize = 20;
            log.warn("Invalid pageSize, using default: 20");
        }

        CaseListResponse response = caseService.getCaseList(userId, pageNumber, pageSize);

        return ResponseEntity.ok(
                ResponseMessage.<CaseListResponse>builder()
                        .status(200)
                        .message("사례 목록을 조회했습니다.")
                        .data(response)
                        .build()
        );
    }

    /**
     * 단일 사례 상세 조회
     * GET /api/v1/cases/{caseId}
     */
    @GetMapping("/{caseId}")
    public ResponseEntity<ResponseMessage<CaseDetailResponse>> getCaseDetail(
            @RequestHeader("userId") Long userId,
            @PathVariable("caseId") Long caseId) {

        log.info("Get case detail request - userId: {}, caseId: {}", userId, caseId);

        CaseDetailResponse response = caseService.getCaseDetail(userId, caseId);

        return ResponseEntity.ok(
                ResponseMessage.<CaseDetailResponse>builder()
                        .status(200)
                        .message("사례 상세 정보를 조회했습니다.")
                        .data(response)
                        .build()
        );
    }

    /**
     * 온보딩 정보 조회 API
     * GET /api/v1/cases/{caseId}/onboarding?accountId={accountId}
     */
    @GetMapping("/{caseId}/onboarding")
    public ResponseEntity<ResponseMessage<OnboardingInfoResponse>> getOnboardingInfo(
            @RequestHeader("userId") Long userId,  // ✅ 다른 API들과 동일하게
            @PathVariable("caseId") Long caseId,
            @RequestParam("accountId") String accountId) {

        log.info("GET /api/v1/cases/{}/onboarding - userId: {}, accountId: {}",
                caseId, userId, accountId);

        OnboardingInfoResponse response = caseService.getOnboardingInfo(caseId, accountId);

        return ResponseEntity.ok(
                ResponseMessage.<OnboardingInfoResponse>builder()
                        .status(200)
                        .message("온보딩 정보를 조회했습니다.")
                        .data(response)
                        .build()
        );
    }
    // CaseController.java에 추가할 메서드

// CaseController.java에 추가

    /**
     * CloudFormation Stack 정보 조회 API
     * GET /api/v1/cases/{caseId}/onboarding/stack?accountId={accountId}
     */
    @GetMapping("/{caseId}/onboarding/stack")
    public ResponseEntity<ResponseMessage<StackInfoResponse>> getStackInfo(
            @RequestHeader("userId") Long userId,
            @PathVariable("caseId") Long caseId,
            @RequestParam("accountId") String accountId) {

        log.info("GET /api/v1/cases/{}/onboarding/stack - userId: {}, accountId: {}",
                caseId, userId, accountId);

        try {
            StackInfoResponse response = caseService.getStackInfo(caseId, accountId);

            return ResponseEntity.ok(
                    ResponseMessage.<StackInfoResponse>builder()
                            .status(200)
                            .message("Stack 정보 조회에 성공했습니다.")
                            .data(response)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            log.warn("Stack info retrieval failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ResponseMessage.<StackInfoResponse>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build()
            );

        } catch (IllegalStateException e) {
            log.warn("Stack not created yet: {}", e.getMessage());
            return ResponseEntity.status(404).body(
                    ResponseMessage.<StackInfoResponse>builder()
                            .status(404)
                            .message(e.getMessage())
                            .build()
            );

        } catch (Exception e) {
            log.error("Stack info retrieval error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ResponseMessage.<StackInfoResponse>builder()
                            .status(500)
                            .message("Stack 정보 조회 중 오류가 발생했습니다.")
                            .build()
            );
        }
    }


    /**
     * 온보딩 정보 이메일 전송 API
     * POST /api/v1/cases/{caseId}/onboarding/email
     */
    @PostMapping("/{caseId}/onboarding/email")
    public ResponseEntity<ResponseMessage<Void>> sendOnboardingEmail(
            @RequestHeader("userId") Long userId,
            @PathVariable("caseId") Long caseId,
            @Valid @RequestBody SendOnboardingEmailRequest request) {

        log.info("POST /api/v1/cases/{}/onboarding/email - userId: {}, email: {}, accountId: {}",
                caseId, userId, request.getEmail(), request.getAccountId());

        caseService.sendOnboardingEmail(caseId, request.getAccountId(), request.getEmail());

        return ResponseEntity.ok(
                ResponseMessage.<Void>builder()
                        .status(200)
                        .message("온보딩 정보가 이메일로 전송되었습니다.")
                        .build()
        );
    }

    /**
     * 배포 상태 조회 API
     * GET /api/v1/cases/{caseId}/deployment/status?accountId={accountId}
     */
    @GetMapping("/{caseId}/deployment/status")
    public ResponseEntity<ResponseMessage<DeploymentStatusResponse>> getDeploymentStatus(
            @RequestHeader("userId") Long userId,
            @PathVariable("caseId") Long caseId,
            @RequestParam("accountId") String accountId) {

        log.info("GET /api/v1/cases/{}/deployment/status - userId: {}, accountId: {}",
                caseId, userId, accountId);

        try {
            DeploymentStatusResponse response = caseService.getDeploymentStatus(caseId, accountId);

            return ResponseEntity.ok(
                    ResponseMessage.<DeploymentStatusResponse>builder()
                            .status(200)
                            .message("배포 상태 조회에 성공했습니다.")
                            .data(response)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            log.warn("Deployment status retrieval failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ResponseMessage.<DeploymentStatusResponse>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build()
            );

        } catch (Exception e) {
            log.error("Deployment status retrieval error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ResponseMessage.<DeploymentStatusResponse>builder()
                            .status(500)
                            .message("배포 상태 조회 중 오류가 발생했습니다.")
                            .build()
            );
        }
    }

    /**
     * 배포 상태 업데이트 API (Webhook 또는 관리자용)
     * PATCH /api/v1/cases/{caseId}/deployment/status
     */
    @PatchMapping("/{caseId}/deployment/status")
    public ResponseEntity<ResponseMessage<Void>> updateDeploymentStatus(
            @RequestHeader("userId") Long userId,
            @PathVariable("caseId") Long caseId,
            @RequestParam("accountId") String accountId,
            @RequestParam("status") String status,
            @RequestParam(value = "statusReason", required = false) String statusReason) {

        log.info("PATCH /api/v1/cases/{}/deployment/status - userId: {}, accountId: {}, status: {}",
                caseId, userId, accountId, status);

        try {
            DeploymentStatus.StackStatus stackStatus = DeploymentStatus.StackStatus.valueOf(status);
            caseService.updateDeploymentStatus(caseId, accountId, stackStatus, statusReason);

            return ResponseEntity.ok(
                    ResponseMessage.<Void>builder()
                            .status(200)
                            .message("배포 상태가 업데이트되었습니다.")
                            .build()
            );

        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: {}", status);
            return ResponseEntity.badRequest().body(
                    ResponseMessage.<Void>builder()
                            .status(400)
                            .message("잘못된 상태 값입니다: " + status)
                            .build()
            );

        } catch (Exception e) {
            log.error("Deployment status update error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ResponseMessage.<Void>builder()
                            .status(500)
                            .message("배포 상태 업데이트 중 오류가 발생했습니다.")
                            .build()
            );
        }
    }

    // SSE API
    @GetMapping(value = "/{caseId}/deployment/status/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeploymentStatus(
            @PathVariable Long caseId,
            @RequestParam String accountId,
            @RequestHeader("userId") Long userId) {

        log.info("SSE connection opened - caseId: {}, accountId: {}, userId: {}",
                caseId, accountId, userId);

        return caseService.streamDeploymentStatus(userId, caseId, accountId);
    }

    // 기존 일반 조회 API도 유지
    @GetMapping("/cases/{caseId}/deployment/status")
    public ResponseEntity<ResponseMessage> getDeploymentStatus(
            @PathVariable Long caseId,
            @RequestParam String accountId,
            @RequestHeader("X-User-Id") Long userId) {

        DeploymentStatusResponse response = caseService.getDeploymentStatus(caseId, accountId);

        return ResponseEntity.ok(ResponseMessage.builder()
                .status(200)
                .message("배포 상태 조회에 성공했습니다.")
                .data(response)
                .build());
    }

    // CaseController.java에 추가

    /**
     * Stack 생성 정보 실시간 조회 (SSE)
     * GET /api/v1/cases/{caseId}/onboarding/stack/stream?accountId={accountId}
     */
    @GetMapping(value = "/{caseId}/onboarding/stack/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStackInfo(
            @PathVariable Long caseId,
            @RequestParam String accountId,
            @RequestHeader("userId") Long userId) {

        log.info("SSE connection opened for stack info - caseId: {}, accountId: {}, userId: {}",
                caseId, accountId, userId);

        return caseService.streamStackInfo(userId, caseId, accountId);
    }


    /**
     * 사례 수정 (부분 수정)
     * PATCH /api/v1/cases/{caseId}
     */
    @PatchMapping("/{caseId}")
    public ResponseEntity<ResponseMessage<CaseUpdateResponse>> updateCase(
            @RequestHeader("userId") Long userId,
            @PathVariable("caseId") Long caseId,
            @RequestBody CaseUpdateRequest request) {

        log.info("Update case request - userId: {}, caseId: {}", userId, caseId);

        CaseUpdateResponse response = caseService.updateCase(userId, caseId, request);

        return ResponseEntity.ok(
                ResponseMessage.<CaseUpdateResponse>builder()
                        .status(200)
                        .message("사례를 성공적으로 수정했습니다.")
                        .data(response)
                        .build()
        );
    }

} // 클래스 닫는 중괄호