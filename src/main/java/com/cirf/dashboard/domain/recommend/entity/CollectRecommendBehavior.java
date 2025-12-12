package com.cirf.dashboard.domain.recommend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

/**
 * cirf-collect-recommend 테이블의 BEHAVIOR 엔티티
 * PK: BEHAVIOR#{behaviorId}
 * SK: METADATA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class CollectRecommendBehavior {

    private String pk;              // Partition Key: BEHAVIOR#{behaviorId}
    private String sk;              // Sort Key: METADATA
    private String itemType;        // "BEHAVIOR"
    private String behaviorId;      // 예: "access_key_exposure"
    private String categoryId;      // 속한 카테고리 ID
    private String title;           // 예: "Access Key Exposure"
    private List<LogMapping> logs;  // 필요한 로그 매핑 정보

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }

    @DynamoDbAttribute("itemType")
    public String getItemType() {
        return itemType;
    }

    @DynamoDbAttribute("behaviorId")
    public String getBehaviorId() {
        return behaviorId;
    }

    @DynamoDbAttribute("categoryId")
    public String getCategoryId() {
        return categoryId;
    }

    @DynamoDbAttribute("title")
    public String getTitle() {
        return title;
    }

    @DynamoDbAttribute("logs")
    public List<LogMapping> getLogs() {
        return logs;
    }
}
