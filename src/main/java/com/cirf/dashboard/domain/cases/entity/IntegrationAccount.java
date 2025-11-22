package com.cirf.dashboard.domain.cases.entity;
// 김도연 새롭게 생성
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationAccount {

    private String pk;  // USER#{user_id}#CASE#{case_id}
    private String sk;  // ACCOUNT#{account_id}
    private String roleArn;
    private Boolean roleCheck;
    private String accountId;
    private Long userId;
    private Long caseId;

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

    @DynamoDbAttribute("role_arn")
    public String getRoleArn() {
        return roleArn;
    }

    @DynamoDbAttribute("role_check")
    public Boolean getRoleCheck() {
        return roleCheck;
    }

    @DynamoDbAttribute("account_id")
    public String getAccountId() { return accountId; }

    @DynamoDbAttribute("user_id")
    public Long getUserId() { return userId; }

    @DynamoDbAttribute("case_id")
    public Long getCaseId() { return caseId; }
}