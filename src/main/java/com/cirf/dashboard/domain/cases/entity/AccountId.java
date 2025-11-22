package com.cirf.dashboard.domain.cases.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@AllArgsConstructor
public class AccountId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "case_id")
    private IncidentCase incidentCase;

    @Column(nullable = false, name = "account_id")
    private String accountId;

    @Column(nullable = false, name = "role_arn")
    private String roleArn;

    @Column(nullable = false, name = "role_check")
    private Boolean roleCheck;

    //김도연 새롭게 추가
    public void setIncidentCase(IncidentCase incidentCase) {
        this.incidentCase = incidentCase;
    }

}

