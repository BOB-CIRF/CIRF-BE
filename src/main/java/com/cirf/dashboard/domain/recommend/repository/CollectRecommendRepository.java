package com.cirf.dashboard.domain.recommend.repository;

import com.cirf.dashboard.domain.recommend.entity.CollectRecommendBehavior;
import com.cirf.dashboard.domain.recommend.entity.CollectRecommendCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CollectRecommendRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "cirf-collect-recommend";
    private static final String METADATA_SK = "METADATA";

    /**
     * 모든 카테고리 조회
     */
    public List<CollectRecommendCategory> findAllCategories() {
        DynamoDbTable<CollectRecommendCategory> table = dynamoDbEnhancedClient.table(
                TABLE_NAME,
                TableSchema.fromBean(CollectRecommendCategory.class)
        );

        log.info("Scanning DynamoDB table: {} for all categories", TABLE_NAME);

        List<CollectRecommendCategory> results = table.scan(builder -> builder
                        .filterExpression(software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                                .expression("itemType = :itemType")
                                .putExpressionValue(":itemType",
                                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                                .s("CATEGORY")
                                                .build())
                                .build())
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();

        log.info("Found {} categories", results.size());
        return results;
    }

    /**
     * 특정 카테고리에 속한 모든 Behavior 조회
     */
    public List<CollectRecommendBehavior> findBehaviorsByCategoryId(String categoryId) {
        DynamoDbTable<CollectRecommendBehavior> table = dynamoDbEnhancedClient.table(
                TABLE_NAME,
                TableSchema.fromBean(CollectRecommendBehavior.class)
        );

        log.info("Scanning DynamoDB table: {} for behaviors with categoryId: {}", TABLE_NAME, categoryId);

        List<CollectRecommendBehavior> results = table.scan(builder -> builder
                        .filterExpression(software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                                .expression("itemType = :itemType AND categoryId = :categoryId")
                                .putExpressionValue(":itemType",
                                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                                .s("BEHAVIOR")
                                                .build())
                                .putExpressionValue(":categoryId",
                                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                                .s(categoryId)
                                                .build())
                                .build())
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();

        log.info("Found {} behaviors for categoryId: {}", results.size(), categoryId);
        return results;
    }

    /**
     * behaviorId로 특정 Behavior 조회
     */
    public Optional<CollectRecommendBehavior> findBehaviorById(String behaviorId) {
        DynamoDbTable<CollectRecommendBehavior> table = dynamoDbEnhancedClient.table(
                TABLE_NAME,
                TableSchema.fromBean(CollectRecommendBehavior.class)
        );

        String pk = String.format("BEHAVIOR#%s", behaviorId);

        log.info("Querying DynamoDB table: {} with PK: {}, SK: {}", TABLE_NAME, pk, METADATA_SK);

        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue(METADATA_SK)
                .build();

        CollectRecommendBehavior behavior = table.getItem(key);

        return Optional.ofNullable(behavior);
    }

    /**
     * behaviorId 리스트로 여러 Behavior 조회
     */
    public List<CollectRecommendBehavior> findBehaviorsByIds(List<String> behaviorIds) {
        DynamoDbTable<CollectRecommendBehavior> table = dynamoDbEnhancedClient.table(
                TABLE_NAME,
                TableSchema.fromBean(CollectRecommendBehavior.class)
        );

        log.info("Querying DynamoDB table: {} for {} behaviors", TABLE_NAME, behaviorIds.size());

        return behaviorIds.stream()
                .map(behaviorId -> {
                    String pk = String.format("BEHAVIOR#%s", behaviorId);
                    Key key = Key.builder()
                            .partitionValue(pk)
                            .sortValue(METADATA_SK)
                            .build();
                    return table.getItem(key);
                })
                .filter(behavior -> behavior != null)
                .toList();
    }
}
