package com.cirf.dashboard.domain.recommend.controller;

import com.cirf.dashboard.domain.recommend.dto.response.*;
import com.cirf.dashboard.domain.recommend.service.CollectRecommendService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/collect-recommend")
@RequiredArgsConstructor
public class CollectRecommendController {

    private final CollectRecommendService collectRecommendService;

    /**
     * 1. 카테고리 리스트 조회
     * GET /api/v1/collect-recommend/categories
     */
    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories(
            @RequestHeader("userId") @NotNull Long userId
    ) {
        log.info("GET /api/v1/collect-recommend/categories - userId: {}", userId);

        List<CategoryResponse> categories = collectRecommendService.getAllCategories();

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_CATEGORIES_SUCCESS.getMessage(),
                categories
        );
    }

    /**
     * 2. 특정 카테고리의 세부 행위 리스트
     * GET /api/v1/collect-recommend/behaviors?categoryId={categoryId}
     */
    @GetMapping("/behaviors")
    public ApiResponse<List<BehaviorSummaryResponse>> getBehaviors(
            @RequestHeader("userId") @NotNull Long userId,
            @RequestParam @NotBlank(message = "categoryId는 필수입니다.") String categoryId
    ) {
        log.info("GET /api/v1/collect-recommend/behaviors - userId: {}, categoryId: {}", userId, categoryId);

        List<BehaviorSummaryResponse> behaviors = collectRecommendService.getBehaviorsByCategory(categoryId);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_BEHAVIORS_SUCCESS.getMessage(),
                behaviors
        );
    }

    /**
     * 3. 세부 행위 상세 (필요 로그 + 근거)
     * GET /api/v1/collect-recommend/behaviors/{behaviorId}
     */
    @GetMapping("/behaviors/{behaviorId}")
    public ApiResponse<BehaviorDetailResponse> getBehaviorDetail(
            @RequestHeader("userId") @NotNull Long userId,
            @PathVariable @NotBlank(message = "behaviorId는 필수입니다.") String behaviorId
    ) {
        log.info("GET /api/v1/collect-recommend/behaviors/{} - userId: {}", behaviorId, userId);

        BehaviorDetailResponse detail = collectRecommendService.getBehaviorDetail(behaviorId);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_BEHAVIOR_DETAIL_SUCCESS.getMessage(),
                detail
        );
    }

    /**
     * 4. 수집 추천 로그 (여러 behaviorId 기반)
     * GET /api/v1/collect-recommend/recommend?mediumIds=access_key_exposure&mediumIds=snapshot_sharing_leak
     */
    @GetMapping("/recommend")
    public ApiResponse<CollectRecommendResponse> getRecommendedLogs(
            @RequestHeader("userId") @NotNull Long userId,
            @RequestParam List<String> mediumIds
    ) {
        log.info("GET /api/v1/collect-recommend/recommend - userId: {}, behaviors count: {}", userId, mediumIds.size());

        CollectRecommendResponse response = collectRecommendService.getRecommendedLogs(mediumIds);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_RECOMMEND_SUCCESS.getMessage(),
                response
        );
    }
}
