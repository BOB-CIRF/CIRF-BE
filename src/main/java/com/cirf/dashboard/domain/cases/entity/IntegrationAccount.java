package com.cirf.dashboard.domain.cases.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationAccount {

    private String pk;  // USER#{user_id}#CASE#{case_id}
    private String sk;  // ACCOUNT#{account_id}
    private String roleArn;
    private Boolean roleCheck;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    @DynamoDbAttribute("roleArn")
    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    @DynamoDbAttribute("roleCheck")
    public Boolean getRoleCheck() {
        return roleCheck;
    }

    public void setRoleCheck(Boolean roleCheck) {
        this.roleCheck = roleCheck;
    }
}