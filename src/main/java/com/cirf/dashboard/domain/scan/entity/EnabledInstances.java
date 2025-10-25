package com.cirf.dashboard.domain.scan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class EnabledInstances {

    private String pk;              // Partition Key: EC2#{ec2ScanId}
    private String sk;              // Sort Key: REG#{region}#INSTANCE#{instanceId}
    private Long ec2ScanId;
    private Long idxId;
    private String instanceId;
    private String instanceName;
    private String instanceType;
    private String region;
    private String status;
    private String publicIp;

    private String gsi4Pk; // EC2#IDX#{idxId}

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "GSI4")
    @DynamoDbAttribute("GSI4PK")
    public String getGsi4Pk() {
        return gsi4Pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }

    @DynamoDbAttribute("ec2_scan_id")
    public Long getEc2ScanId() {
        return ec2ScanId;
    }

    @DynamoDbAttribute("idx_id")
    public Long getIdxId() { return idxId; }

    @DynamoDbAttribute("instance_id")
    public String getInstanceId() { return instanceId; }

    @DynamoDbAttribute("instance_name")
    public String getInstanceName() { return instanceName; }

    @DynamoDbAttribute("instance_type")
    public String getInstanceType() { return instanceType; }

    @DynamoDbAttribute("region")
    public String getRegion() { return region; }

    @DynamoDbAttribute("status")
    public String getStatus() { return status; }

    @DynamoDbAttribute("public_ip")
    public String getPublicIp() { return publicIp; }

    public void updatePk(String pk) { this.pk = pk; }

    public void updateSk(String sk) { this.sk = sk; }

    public void updateEc2ScanId(Long ec2ScanId) { this.ec2ScanId = ec2ScanId; }

    public void updateIdxId(Long idxId) { this.idxId = idxId; }

    public void updateInstanceId(String instanceId) { this.instanceId = instanceId; }

    public void updateInstanceName(String instanceName) { this.instanceName = instanceName; }

    public void updateInstanceType(String instanceType) { this.instanceType = instanceType; }

    public void updateRegion(String region) { this.region = region; }

    public void updateStatus(String status) { this.status = status; }

    public void updatePublicIp(String publicIp) { this.publicIp = publicIp; }

    public void updateGsi4Pk(String gsi4Pk) { this.gsi4Pk = gsi4Pk; }

}
