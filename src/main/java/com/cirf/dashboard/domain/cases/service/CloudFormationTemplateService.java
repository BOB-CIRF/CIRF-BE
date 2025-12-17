package com.cirf.dashboard.domain.cases.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * CloudFormation 템플릿 생성 및 S3 업로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudFormationTemplateService {

    @Value("${aws.account-id}")
    private String CIRF_ACCOUNT_ID;

    @Value("${aws.lambda.onboarding-function-name}")
    private String ONBOARDING_LAMBDA_NAME;

    @Value("${aws.kms-key-id}")
    private String KMS_KEY_ID;

    @Value("${aws.notification-arn}")
    private String NOTIFICATION_ARN;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /**
     * CloudFormation 템플릿을 생성하고 S3에 업로드
     *
     * @param caseId 사례 ID
     * @param accountId 고객 계정 ID
     * @param caseBucketName 케이스별 S3 버킷명 (예: cirf-case-26-1764830021024)
     * @return S3에 저장된 템플릿 URL
     */
    public String createAndUploadTemplate(Long caseId, String accountId, String caseBucketName) {
        try {
            // 1. 템플릿 생성
            String template = generateCloudFormationTemplate(caseId, accountId);

            // 2. S3에 업로드 (케이스별 버킷 사용)
            String s3Key = String.format("cloudformation-templates/case-%d/account-%s/template.yml",
                    caseId, accountId);
            uploadToS3(caseBucketName, s3Key, template);

            // 3. S3 URL 반환
            String s3Url = String.format("s3://%s/%s", caseBucketName, s3Key);
            log.info("CloudFormation template uploaded successfully - caseId: {}, s3Url: {}", caseId, s3Url);

            return s3Url;

        } catch (Exception e) {
            log.error("Failed to create and upload CloudFormation template - caseId: {}, accountId: {}",
                    caseId, accountId, e);
            throw new RuntimeException("CloudFormation 템플릿 생성 및 업로드 실패", e);
        }
    }

    /**
     * S3 Presigned URL 생성
     *
     * @param bucketName S3 버킷 이름
     * @param s3Key S3 객체 키
     * @param expirationMinutes 만료 시간(분)
     * @return Presigned URL
     */
    public String generatePresignedUrl(String bucketName, String s3Key, int expirationMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest =
                    s3Presigner.presignGetObject(presignRequest);

            URL presignedUrl = presignedRequest.url();

            log.info("Generated presigned URL for {}/{}, expires in {} minutes",
                    bucketName, s3Key, expirationMinutes);

            return presignedUrl.toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage(), e);
            throw new RuntimeException("Presigned URL 생성 실패", e);
        }
    }


    /**
     * CloudFormation 런치 링크 생성
     *
     * @param presignedUrl S3 Presigned URL
     * @param caseId 사례 ID
     * @param customerAccountId 고객 계정 ID
     * @return CloudFormation 콘솔 런치 링크
     */
    public String generateCloudFormationLaunchUrl(
            String presignedUrl,
            Long caseId,
            String customerAccountId
    ) {
        String encodedPresignedUrl = URLEncoder.encode(presignedUrl, StandardCharsets.UTF_8);
        String stackName = String.format("CIRF-Case-%d-Account-%s", caseId, customerAccountId);

        // 🔥 Notification ARN
        String encodedNotificationArn =
                URLEncoder.encode(NOTIFICATION_ARN, StandardCharsets.UTF_8);

        String launchUrl = String.format(
                "https://console.aws.amazon.com/cloudformation/home?region=ap-northeast-2#/stacks/create/review?" +
                        "templateURL=%s&" +
                        "stackName=%s&" +
                        "param_CIRFAccountId=%s&" +
                        "param_KMSKeyId=%s&" +
                        "param_CaseId=%s&" +
                        "param_OnboardingLambdaName=%s&" +
                        "notificationARNs[0]=%s",
                encodedPresignedUrl,
                stackName,
                CIRF_ACCOUNT_ID,
                KMS_KEY_ID,
                caseId,
                ONBOARDING_LAMBDA_NAME,
                encodedNotificationArn
        );

        log.info("Generated CloudFormation launch URL for case {} and account {}", caseId, customerAccountId);
        return launchUrl;
    }


    // ✅ 여기에 추가
    /**
     * 템플릿 내용만 반환 (프론트엔드에서 확인용)
     */
    public String getTemplateContent(Long caseId, String customerAccountId) {
        return generateCloudFormationTemplate(caseId, customerAccountId);
    }

    /**
     * CloudFormation 템플릿 생성
     */
    private String generateCloudFormationTemplate(Long caseId, String accountId) {
        String template = "AWSTemplateFormatVersion: '2010-09-09'\n" +
                "Description: CIRF Onboarding Template - Create IRAutomationRole and Scan Trigger\n" +
                "\n" +
                "Parameters:\n" +
                "  CIRFAccountId:\n" +
                "    Type: String\n" +
                "    Default: \"%s\"\n" +
                "    Description: CIRF Account ID (analyst account)\n" +
                "    AllowedPattern: ^\\d{12}$\n" +
                "\n" +
                "  KMSKeyId:\n" +
                "    Type: String\n" +
                "    Default: \"%s\"\n" +
                "    Description: KMS Key ID for encryption in CIRF account\n" +
                "\n" +
                "  CaseId:\n" +
                "    Type: String\n" +
                "    Default: \"%s\"\n" +
                "    Description: Case ID for this onboarding\n" +
                "\n" +
                "  OnboardingLambdaName:\n" +
                "    Type: String\n" +
                "    Default: \"%s\"\n" +
                "    Description: Name of the onboarding Lambda function\n" +
                "\n" +
                "Resources:\n" +
                "  IRAutomationRole:\n" +
                "    Type: AWS::IAM::Role\n" +
                "    Properties:\n" +
                "      RoleName: IRAutomationRole\n" +
                "      AssumeRolePolicyDocument:\n" +
                "        Version: '2012-10-17'\n" +
                "        Statement:\n" +
                "          - Sid: ScanLogsAndEc2\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS:\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/CIRFBackendRole\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/ScanListRegions-role-8xsjkk2f\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/ScanLambda-role-c8mkdkbn\"\n" +
                "            Action: sts:AssumeRole\n" +
                "          - Sid: S3CollectLogs\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS:\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/LC-S3-PrecheckS3Data-role-zvsv6tvl\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/LC-S3-StartS3BatchJob-role-3iw1rpzp\"\n" +
                "            Action: sts:AssumeRole\n" +
                "          - Sid: AllowCIRFAccountToAssumeRole\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS: !Sub \"arn:aws:iam::${CIRFAccountId}:role/CIRF-Collector-Role\"\n" +
                "            Action: sts:AssumeRole\n" +
                "          - Sid: CTcollectlogs\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS: !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/collectCTLogs-role-ald45ohu\"\n" +
                "            Action: sts:AssumeRole\n" +
                "          - Sid: CWcollectlogs\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS:\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/LC-CW-GenerateManifest-role-2si1zk9c\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/LC-CW-ProcessGenericLogs-role-lg8oxges\"\n" +
                "            Action: sts:AssumeRole\n" +
                "          - Sid: EC2Snapshot\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS:\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/ResolveSrcVolss-role-skpki9cf\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/ShareSnapshotWithTarget-role-ygxe89tu\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/CreateSrcSnapshotss-role-hd6kawee\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/CheckSnapshotStatus-role-zr46ewox\"\n" +
                "            Action: sts:AssumeRole\n" +
                "          - Sid: EC2MemoryDump\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS:\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/DumpLinux-role-53hwapl5\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/GetFacts-role-rqggdeuv\"\n" +
                "                - !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/ResolveSrcVolss-role-skpki9cf\"\n" +
                "            Action: sts:AssumeRole\n" +
                "          - Sid: OnboardingLambda\n" +
                "            Effect: Allow\n" +
                "            Principal:\n" +
                "              AWS: !Sub \"arn:aws:iam::${CIRFAccountId}:role/service-role/Onboarding_lambda-role-zld8b21w\"\n" +
                "            Action: sts:AssumeRole\n" +
                "      Policies:\n" +
                "        - PolicyName: CloudTrailEventHistoryReadPolicy\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Sid: AllowCloudTrailEventHistoryRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - cloudtrail:LookupEvents\n" +
                "                  - cloudtrail:GetTrail\n" +
                "                  - cloudtrail:DescribeTrails\n" +
                "                Resource: \"*\"\n" +
                "        - PolicyName: CloudwatchLogcollectPolicy\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Sid: AllowCloudwatchLogcollect\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - logs:StartQuery\n" +
                "                  - logs:GetQueryResults\n" +
                "                  - logs:StopQuery\n" +
                "                  - logs:DescribeLogGroups\n" +
                "                  - logs:DescribeLogStreams\n" +
                "                  - logs:GetLogEvents\n" +
                "                Resource: \"*\"\n" +
                "              - Effect: Allow\n" +
                "                Action:\n" +
                "                  - s3:PutObject\n" +
                "                  - s3:AbortMultipartUpload\n" +
                "                  - s3:ListBucketMultipartUploads\n" +
                "                Resource:\n" +
                "                  - \"*\"\n" +
                "              - Effect: Allow\n" +
                "                Action:\n" +
                "                  - kms:Encrypt\n" +
                "                  - kms:GenerateDataKey\n" +
                "                Resource:\n" +
                "                  - !Sub \"arn:aws:kms:ap-northeast-2:${CIRFAccountId}:key/${KMSKeyId}\"\n" +
                "        - PolicyName: EC2MemoryDumpPolicy\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Effect: Allow\n" +
                "                Action: ssm:DescribeInstanceInformation\n" +
                "                Resource: \"*\"\n" +
                "              - Effect: Allow\n" +
                "                Action: ec2:DescribeInstances\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: AllowSendCommandOnApprovedDocs\n" +
                "                Effect: Allow\n" +
                "                Action: ssm:SendCommand\n" +
                "                Resource:\n" +
                "                  - \"arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript\"\n" +
                "                  - \"arn:aws:ssm:ap-northeast-2::document/AWS-RunPowerShellScript\"\n" +
                "              - Sid: AllowSendCommandToAllInstances\n" +
                "                Effect: Allow\n" +
                "                Action: ssm:SendCommand\n" +
                "                Resource: !Sub \"arn:aws:ec2:ap-northeast-2:${AWS::AccountId}:instance/*\"\n" +
                "              - Sid: ReadCommandResults\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - ssm:GetCommandInvocation\n" +
                "                Resource: \"*\"\n" +
                "        - PolicyName: EC2SnapshotPolicy\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Sid: DescribeInstances\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - ec2:DescribeInstances\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: DescribeVolumes\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - ec2:DescribeVolumes\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: CreateSnapshot\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - ec2:CreateSnapshot\n" +
                "                  - ec2:CreateTags\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: DescribeSnapshots\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - ec2:DescribeSnapshots\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: KMSForEncryptedVolumes\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - kms:Decrypt\n" +
                "                  - kms:DescribeKey\n" +
                "                  - kms:CreateGrant\n" +
                "                Resource:\n" +
                "                  - !Sub \"arn:aws:kms:ap-northeast-2:${AWS::AccountId}:key/*\"\n" +
                "                Condition:\n" +
                "                  StringEquals:\n" +
                "                    kms:ViaService:\n" +
                "                      - \"ec2.ap-northeast-2.amazonaws.com\"\n" +
                "        - PolicyName: S3CollectLogsPolicy\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Sid: CollectLogFromS3\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - s3:ListBucket\n" +
                "                Resource: \"*\"\n" +
                "        - PolicyName: ScanEc2Policy\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Effect: Allow\n" +
                "                Action:\n" +
                "                  - ec2:DescribeInstances\n" +
                "                  - ec2:DescribeRegions\n" +
                "                Resource: \"*\"\n" +
                "        - PolicyName: ScanLogPolicy\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Sid: STSReadIdentity\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - sts:GetCallerIdentity\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: CloudTrailRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - cloudtrail:ListTrails\n" +
                "                  - cloudtrail:GetTrail\n" +
                "                  - cloudtrail:GetTrailStatus\n" +
                "                  - cloudtrail:ListEventDataStores\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: S3BucketMetaRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - s3:ListAllMyBuckets\n" +
                "                  - s3:GetBucketLocation\n" +
                "                  - s3:GetBucketLogging\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: EC2FlowLogsDescribe\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - ec2:DescribeFlowLogs\n" +
                "                  - ec2:DescribeRegions\n" +
                "                  - ec2:DescribeVpcs\n" +
                "                  - ec2:DescribeClientVpnEndpoints\n" +
                "                  - ec2:DescribeTransitGateways\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: ApiGatewayControlPlaneRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - apigateway:GET\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: ELBv2Describe\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - elasticloadbalancing:DescribeLoadBalancers\n" +
                "                  - elasticloadbalancing:DescribeLoadBalancerAttributes\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: CloudFrontRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - cloudfront:ListDistributions\n" +
                "                  - cloudfront:GetDistributionConfig\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: RDSDescribe\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - rds:DescribeDBInstances\n" +
                "                  - rds:DescribeDBClusters\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: LambdaList\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - lambda:ListFunctions\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: GuardDutyRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - guardduty:ListDetectors\n" +
                "                  - guardduty:GetDetector\n" +
                "                  - guardduty:ListPublishingDestinations\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: ConfigRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - config:DescribeConfigurationRecorders\n" +
                "                  - config:DescribeConfigurationRecorderStatus\n" +
                "                  - config:DescribeDeliveryChannels\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: Route53QueryLogsList\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - route53:ListQueryLoggingConfigs\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: Route53ResolverRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - route53resolver:ListResolverQueryLogConfigs\n" +
                "                  - route53resolver:ListResolverQueryLogConfigAssociations\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: WAFv2Read\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - wafv2:ListLoggingConfigurations\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: EKSRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - eks:ListClusters\n" +
                "                  - eks:DescribeCluster\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: CloudWatchLogsRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - logs:DescribeSubscriptionFilters\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: FirehoseRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - firehose:DescribeDeliveryStream\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: RedshiftRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - redshift:DescribeClusters\n" +
                "                  - redshift:DescribeLoggingStatus\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: FSxRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - fsx:DescribeFileSystems\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: WorkSpacesRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - workspaces:DescribeWorkspaceDirectories\n" +
                "                  - workspaces:DescribeWorkspaces\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: NetworkFirewallRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - network-firewall:ListFirewalls\n" +
                "                  - network-firewall:DescribeLoggingConfiguration\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: SSMRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - ssm:GetDocument\n" +
                "                Resource: \"*\"\n" +
                "              - Sid: GlobalAcceleratorRead\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - globalaccelerator:ListAccelerators\n" +
                "                  - globalaccelerator:DescribeAcceleratorAttributes\n" +
                "                Resource: \"*\"\n" +
                "        - PolicyName: S3BucketPolicyManagement\n" +
                "          PolicyDocument:\n" +
                "            Version: '2012-10-17'\n" +
                "            Statement:\n" +
                "              - Sid: ManageS3BucketPolicy\n" +
                "                Effect: Allow\n" +
                "                Action:\n" +
                "                  - s3:GetBucketPolicy\n" +
                "                  - s3:PutBucketPolicy\n" +
                "                  - s3:GetBucketLocation\n" +
                "                Resource: \"arn:aws:s3:::*\"\n" +
                "\n" +
                "  CirfOnboardingHook:\n" +
                "    Type: Custom::CIRFOnboarding\n" +
                "    Properties:\n" +
                "      ServiceToken: !Sub \"arn:aws:lambda:ap-northeast-2:${CIRFAccountId}:function:${OnboardingLambdaName}\"\n" +
                "      AccountId: !Ref AWS::AccountId\n" +
                "      RoleArn: !GetAtt IRAutomationRole.Arn\n" +
                "      CaseId: !Ref CaseId\n" +
                "\n" +
                "Outputs:\n" +
                "  IRAutomationRoleArn:\n" +
                "    Value: !GetAtt IRAutomationRole.Arn\n" +
                "    Export:\n" +
                "      Name: IRAutomationRoleArn\n";

        // 파라미터 치환
        return String.format(template,
                CIRF_ACCOUNT_ID,
                KMS_KEY_ID,
                caseId.toString(),
                ONBOARDING_LAMBDA_NAME);
    }

    /**
     * S3에 템플릿 업로드
     */
    private void uploadToS3(String bucketName, String key, String content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/yml")
                .build();

        RequestBody requestBody = RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8));

        s3Client.putObject(putObjectRequest, requestBody);

        log.info("Template uploaded to S3 - bucket: {}, key: {}", bucketName, key);
    }
}