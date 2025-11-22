package com.cirf.dashboard.domain.cases.entity;

import com.cirf.dashboard.global.entity.BaseTimeEntity;
import com.cirf.dashboard.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Builder
public class IncidentCase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, name = "case_name")
    private String caseName;

    private String description;

    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    @OneToMany(mappedBy = "incidentCase", fetch = FetchType.LAZY)
    private List<AccountId> accountIdList;


    // (김도연) 새롭게 추가
    /**
     * 사례 생성을 위한 정적 팩토리 메서드
     * 기존 엔티티 구조를 유지하면서 생성 로직 추가
     */
    public static IncidentCase createForCaseRegistration(
            String caseName,
            String caseDescription,
            Long userId,
            List<Long> accountIds) {

        IncidentCase incidentCase = new IncidentCase();
        incidentCase.caseName = caseName;
        incidentCase.description = caseDescription;

        // CaseStatus 초기값 설정
        // 실제 CaseStatus enum에 있는 값으로 변경 필요
        // 예시: PENDING, CREATED, ACTIVE, OPEN 등
        // incidentCase.status = CaseStatus.PENDING;  // 또는 프로젝트에 있는 초기 상태값

        // status를 null로 두거나, 기본값을 설정하지 않고 서비스 레이어에서 설정하도록 함
        // 또는 @PrePersist에서 기본값 설정

        return incidentCase;
    }

    /**
     * User 엔티티 설정 메서드
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * 상태 설정 메서드
     */
    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    /**
     * 상태 업데이트 메서드
     */
    public void updateStatus(CaseStatus status) {
        this.status = status;
    }

    /**
     * 사례 정보 수정 메서드
     */
    public void updateCaseInfo(String caseName, String description) {
        if (caseName != null && !caseName.isBlank()) {
            this.caseName = caseName;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }

    /**
     * 초기 상태 설정을 위한 헬퍼 메서드
     * CaseStatus enum의 첫 번째 값을 기본값으로 사용
     */
    public void initializeStatus() {
        if (this.status == null && CaseStatus.values().length > 0) {
            this.status = CaseStatus.values()[0];
        }
    }
}