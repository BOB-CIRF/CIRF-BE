package com.cirf.dashboard.domain.cases.entity;

import com.cirf.dashboard.global.entity.BaseTimeEntity;
import com.cirf.dashboard.global.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
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
}
