package com.cirf.dashboard.domain.cases.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseBucket {

    private String pk;  // BUCKET#USER#{user_id}#CASE#{case_id}
    private String sk;  // METADATA
    private Long userId;
    private Long caseId;
    private String bucketName;

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

    @DynamoDbAttribute("user_id")
    public Long getUserId() { return userId; }

    @DynamoDbAttribute("case_id")
    public Long getCaseId() { return caseId; }

    @DynamoDbAttribute("bucket_name")
    public String getBucketName() { return bucketName; }

}
