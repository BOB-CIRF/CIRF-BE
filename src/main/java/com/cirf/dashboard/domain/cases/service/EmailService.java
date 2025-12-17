package com.cirf.dashboard.domain.cases.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesClient sesClient;
    private final CloudFormationTemplateService cloudFormationTemplateService;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    /**
     * 온보딩 정보를 여러 이메일로 전송
     *
     * @param toEmails 받는 사람 이메일 리스트
     * @param caseId 사례 ID
     * @param accountId 계정 ID
     * @param launchUrl CloudFormation 런치 링크
     * @param presignedUrl Presigned URL
     * @param templateContent 템플릿 내용
     */
    public void sendOnboardingEmail(
            List<String> toEmails,
            Long caseId,
            String accountId,
            String launchUrl,
            String presignedUrl,
            String templateContent) {

        try {
            String subject = String.format("[CIRF] 온보딩 안내 - Case %d (Account: %s)", caseId, accountId);
            String cliCommand = cloudFormationTemplateService.generateCliCommand(caseId, accountId, presignedUrl);
            String htmlBody = generateEmailHtml(caseId, accountId, launchUrl, presignedUrl, templateContent, cliCommand);
            String textBody = generateEmailText(caseId, accountId, launchUrl, presignedUrl, cliCommand);

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toEmails)  // ✅ List<String>으로 변경
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .charset("UTF-8")
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .charset("UTF-8")
                                            .data(htmlBody)
                                            .build())
                                    .text(Content.builder()
                                            .charset("UTF-8")
                                            .data(textBody)
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Email sent successfully to {} - MessageId: {}", toEmails, response.messageId());

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmails, e.getMessage(), e);
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    /**
     * HTML 이메일 본문 생성
     */
    private String generateEmailHtml(
            Long caseId,
            String accountId,
            String launchUrl,
            String presignedUrl,
            String templateContent,
            String cliCommand) {

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .button { 
                            display: inline-block;
                            padding: 12px 24px;
                            margin: 10px 0;
                            background-color: #4CAF50;
                            color: white;
                            text-decoration: none;
                            border-radius: 4px;
                        }
                        .info-box { 
                            background-color: white;
                            padding: 15px;
                            margin: 10px 0;
                            border-left: 4px solid #4CAF50;
                        }
                        .code-box {
                            background-color: #f4f4f4;
                            padding: 15px;
                            border-radius: 4px;
                            overflow-x: auto;
                            font-family: monospace;
                            font-size: 12px;
                            max-height: 400px;
                            overflow-y: auto;
                            cursor: text;
                            user-select: all;
                            -webkit-user-select: all;
                            -moz-user-select: all;
                            -ms-user-select: all;
                        }
                        .code-box pre {
                            margin: 0;
                            white-space: pre;
                            word-wrap: normal;
                        }
                        .footer { 
                            text-align: center;
                            padding: 20px;
                            color: #777;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>CIRF 온보딩 안내</h1>
                        </div>
                        
                        <div class="content">
                            <h2>안녕하세요!</h2>
                            <p>CIRF 플랫폼 온보딩을 위한 CloudFormation 템플릿 정보를 보내드립니다.</p>
                            
                            <div class="info-box">
                                <strong>사례 ID:</strong> %d<br>
                                <strong>AWS 계정 ID:</strong> %s
                            </div>
                            
                            <h3>🚀 배포 방법</h3>
                            <p><strong>옵션 1: AWS 콘솔에서 직접 배포 (권장)</strong></p>
                            <a href="%s" class="button" style="color: white;">AWS에서 배포하기</a>
                            <p style="font-size: 12px; color: #666;">
                                ⚠️ 위 버튼을 클릭하면 AWS CloudFormation 콘솔로 이동하며,<br>
                                모든 파라미터가 자동으로 입력되어 있습니다.
                            </p>
                            
                            <p><strong>옵션 2: AWS CLI로 배포</strong></p>
                            <p>AWS CLI를 사용하여 직접 배포할 수 있습니다.</p>
                            <p style="font-size: 12px; color: #666;">
                                💡 아래 코드 박스를 클릭하면 전체 명령어가 선택됩니다. 복사(Ctrl+C 또는 Cmd+C) 후 터미널에 붙여넣기 하세요.
                            </p>
                            <div class="code-box">
                                <pre>%s</pre>
                            </div>

                            <p><strong>옵션 3: 템플릿 다운로드</strong></p>
                            <p>아래 링크에서 CloudFormation 템플릿을 다운로드할 수 있습니다 (7일간 유효):</p>
                            <a href="%s" style="color: #4CAF50; word-break: break-all;">템플릿 다운로드</a>

                            <h3>📋 CloudFormation 템플릿 미리보기</h3>
                            <div class="code-box">
                                <pre>%s</pre>
                            </div>
                            
                            <h3>📌 다음 단계</h3>
                            <ol>
                                <li><strong>"AWS에서 배포하기"</strong> 버튼을 클릭합니다.</li>
                                <li>AWS 콘솔에 로그인합니다 (계정: %s).</li>
                                <li>파라미터를 확인하고 <strong>"Create stack"</strong>을 클릭합니다.</li>
                                <li>배포 완료까지 약 3-5분 소요됩니다.</li>
                                <li>배포 완료 후 CIRF 플랫폼에서 온보딩이 자동으로 완료됩니다.</li>
                            </ol>
                            
                            <div class="info-box" style="border-left-color: #ff9800;">
                                <strong>⏰ 중요:</strong> Presigned URL은 7일 후 만료됩니다.<br>
                                가능한 빠른 시일 내에 배포를 완료해주세요.
                            </div>
                        </div>
                        
                        <div class="footer">
                            <p>본 이메일은 CIRF 플랫폼에서 자동으로 발송되었습니다.</p>
                            <p>문의사항이 있으시면 지원팀에 연락해주세요.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                caseId,
                accountId,
                launchUrl,
                escapeHtml(cliCommand),
                presignedUrl,
                escapeHtml(templateContent),
                accountId
        );
    }

    /**
     * 텍스트 이메일 본문 생성 (HTML을 지원하지 않는 이메일 클라이언트용)
     */
    private String generateEmailText(
            Long caseId,
            String accountId,
            String launchUrl,
            String presignedUrl,
            String cliCommand) {

        return String.format("""
                CIRF 온보딩 안내
                
                안녕하세요!
                CIRF 플랫폼 온보딩을 위한 CloudFormation 템플릿 정보를 보내드립니다.
                
                사례 ID: %d
                AWS 계정 ID: %s
                
                배포 방법:
                
                1. AWS 콘솔에서 직접 배포 (권장)
                   다음 링크를 브라우저에 붙여넣으세요:
                   %s

                2. AWS CLI로 배포
                   아래 명령어를 복사해서 터미널에서 실행하세요.
                   (명령어를 드래그하여 전체 선택 후 복사하세요)

                   %s

                3. 템플릿 다운로드
                   다음 링크에서 템플릿을 다운로드할 수 있습니다 (7일간 유효):
                   %s
                
                다음 단계:
                1. 위의 배포 링크를 클릭합니다.
                2. AWS 콘솔에 로그인합니다 (계정: %s).
                3. 파라미터를 확인하고 "Create stack"을 클릭합니다.
                4. 배포 완료까지 약 3-5분 소요됩니다.
                
                중요: Presigned URL은 7일 후 만료됩니다.
                
                본 이메일은 CIRF 플랫폼에서 자동으로 발송되었습니다.
                문의사항이 있으시면 지원팀에 연락해주세요.
                """,
                caseId,
                accountId,
                launchUrl,
                cliCommand,
                presignedUrl,
                accountId
        );
    }

    /**
     * HTML 특수문자 이스케이프
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}