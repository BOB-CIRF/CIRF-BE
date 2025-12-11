package com.cirf.dashboard.domain.recommend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * cirf-collect-recommend 테이블의 CATEGORY 엔티티
 * PK: CATEGORY#{categoryId}
 * SK: METADATA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class CollectRecommendCategory {

    private String pk;              // Partition Key: CATEGORY#{categoryId}
    private String sk;              // Sort Key: METADATA
    private String itemType;        // "CATEGORY"
    private String categoryId;      // 예: "credential", "data_exfil"
    private String name;            // 예: "자격 증명 관련"

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

    @DynamoDbAttribute("categoryId")
    public String getCategoryId() {
        return categoryId;
    }

    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }
}
