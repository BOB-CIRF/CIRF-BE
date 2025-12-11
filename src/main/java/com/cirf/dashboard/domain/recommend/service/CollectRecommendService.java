package com.cirf.dashboard.domain.recommend.service;

import com.cirf.dashboard.domain.recommend.dto.response.*;
import com.cirf.dashboard.domain.recommend.entity.CollectRecommendBehavior;
import com.cirf.dashboard.domain.recommend.entity.CollectRecommendCategory;
import com.cirf.dashboard.domain.recommend.entity.LogMapping;
import com.cirf.dashboard.domain.recommend.exception.BehaviorNotFoundException;
import com.cirf.dashboard.domain.recommend.exception.CategoryNotFoundException;
import com.cirf.dashboard.domain.recommend.exception.ErrorMessage;
import com.cirf.dashboard.domain.recommend.repository.CollectRecommendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectRecommendService {

    private final CollectRecommendRepository collectRecommendRepository;

    /**
     * 1. 카테고리 리스트 조회
     */
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");

        List<CollectRecommendCategory> categories = collectRecommendRepository.findAllCategories();

        if (categories.isEmpty()) {
            log.warn("No categories found");
            throw new CategoryNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND);
        }

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * 2. 특정 카테고리의 세부 행위 리스트
     */
    public List<BehaviorSummaryResponse> getBehaviorsByCategory(String categoryId) {
        log.info("Fetching behaviors for categoryId: {}", categoryId);

        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_CATEGORY_ID.getMessage());
        }

        List<CollectRecommendBehavior> behaviors = collectRecommendRepository.findBehaviorsByCategoryId(categoryId);

        if (behaviors.isEmpty()) {
            log.warn("No behaviors found for categoryId: {}", categoryId);
            throw new BehaviorNotFoundException(ErrorMessage.BEHAVIOR_NOT_FOUND);
        }

        return behaviors.stream()
                .map(BehaviorSummaryResponse::from)
                .toList();
    }

    /**
     * 3. 세부 행위 상세 (필요 로그 + 근거)
     */
    public BehaviorDetailResponse getBehaviorDetail(String behaviorId) {
        log.info("Fetching behavior detail for behaviorId: {}", behaviorId);

        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_BEHAVIOR_ID.getMessage());
        }

        CollectRecommendBehavior behavior = collectRecommendRepository.findBehaviorById(behaviorId)
                .orElseThrow(() -> {
                    log.warn("Behavior not found for behaviorId: {}", behaviorId);
                    return new BehaviorNotFoundException(ErrorMessage.BEHAVIOR_NOT_FOUND);
                });

        return BehaviorDetailResponse.from(behavior);
    }

    /**
     * 4. 수집 추천 로그 (여러 behaviorId 기반)
     */
    public CollectRecommendResponse getRecommendedLogs(List<String> behaviorIds) {
        log.info("Fetching recommended logs for {} behaviors", behaviorIds.size());

        if (behaviorIds == null || behaviorIds.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessage.EMPTY_BEHAVIOR_IDS.getMessage());
        }

        // behaviorId 리스트로 Behavior들 조회
        List<CollectRecommendBehavior> behaviors = collectRecommendRepository.findBehaviorsByIds(behaviorIds);

        if (behaviors.isEmpty()) {
            log.warn("No behaviors found for given behaviorIds");
            throw new BehaviorNotFoundException(ErrorMessage.BEHAVIOR_NOT_FOUND);
        }

        // 모든 로그를 수집하고 중복 제거 (logType 기준)
        Map<String, LogMapping> uniqueLogs = new LinkedHashMap<>();

        for (CollectRecommendBehavior behavior : behaviors) {
            if (behavior.getLogs() != null) {
                for (LogMapping log : behavior.getLogs()) {
                    // 같은 logType이면 덮어쓰기 (중복 제거)
                    uniqueLogs.putIfAbsent(log.getLogType(), log);
                }
            }
        }

        // RecommendedLogResponse 리스트 생성
        List<RecommendedLogResponse> recommendedLogs = uniqueLogs.values().stream()
                .map(log -> RecommendedLogResponse.builder()
                        .logType(log.getLogType())
                        .displayName(log.getDisplayName())
                        .build())
                .toList();

        log.info("Found {} unique recommended logs", recommendedLogs.size());

        return CollectRecommendResponse.builder()
                .recommendedLogs(recommendedLogs)
                .build();
    }
}
