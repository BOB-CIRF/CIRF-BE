package com.cirf.dashboard.domain.scan.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LogType {
    CLOUDTRAIL("cloudtrail"),
    S3_ACCESS("s3 access logs"),
    VPC("vpc flow logs"),
    API_GW("api gateway access log"),
    ELB("elb"),
    CLOUDFRONT("cloudfront"),
    CONFIG("config"),
    RDS_AUDIT("rds audit"),
    REDSHIFT_AUDIT("redshift audit"),
    FSX("fsx audit"),
    CLIENT_VPN("client vpn"),
    ROUTE53_RESOLVER("route53 resolver"),
    WAF("waf"),
    EKS("eks"),
    FW_ALERT("network firewall alert logs"),
    FW_FLOW("network firewall flow logs"),
    TRANSIT_GW("transit gateway flow logs"),
    SSM("session manager logs"),
    GLOBAL_ACCELERATOR("global accelerator flow logs");

    private final String type;
}
