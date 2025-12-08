package com.cirf.dashboard.domain.cases.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "deployment_status")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DeploymentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String stackName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StackStatus stackStatus;

    @Column(length = 1000)
    private String statusReason;

    @Column(length = 500)
    private String roleArn;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void updateStatus(StackStatus stackStatus, String statusReason) {
        this.stackStatus = stackStatus;
        this.statusReason = statusReason;
    }

    public enum StackStatus {
        NOT_DEPLOYED,           // 배포 안 됨
        DEPLOYMENT_PENDING,     // 배포 대기 중
        DEPLOYING,             // 배포 중
        DEPLOYED,              // 배포 완료
        DEPLOYMENT_FAILED,     // 배포 실패
        ROLLBACK_IN_PROGRESS,  // 롤백 중
        ROLLBACK_COMPLETE      // 롤백 완료
    }
}