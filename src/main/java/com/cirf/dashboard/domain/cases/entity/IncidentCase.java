package com.cirf.dashboard.domain.cases.entity;

import com.cirf.dashboard.global.entity.BaseTimeEntity;
import com.cirf.dashboard.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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

    @OneToMany(mappedBy = "incidentCase", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccountId> accountIdList = new ArrayList<>();

    public void updateCaseInfo(String caseName, String description) {
        if (caseName != null && !caseName.isBlank()) {
            this.caseName = caseName;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }
}