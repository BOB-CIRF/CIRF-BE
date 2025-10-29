package com.cirf.dashboard.domain.cases.controller;

import com.cirf.dashboard.domain.cases.controller.ResponseMessage;
import com.cirf.dashboard.domain.cases.dto.request.CaseCreateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseCreateResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDeleteResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseListResponse;
import com.cirf.dashboard.domain.cases.dto.response.CaseDetailResponse;
import com.cirf.dashboard.domain.cases.service.CaseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.cirf.dashboard.domain.cases.dto.request.CaseUpdateRequest;
import com.cirf.dashboard.domain.cases.dto.response.CaseUpdateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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