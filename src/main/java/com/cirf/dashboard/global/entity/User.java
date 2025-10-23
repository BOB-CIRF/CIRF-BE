package com.cirf.dashboard.global.entity;

import com.cirf.dashboard.domain.cases.entity.IncidentCase;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false, name = "user_name")
    private String userName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, name = "login_id")
    private String loginId;

    @OneToMany(mappedBy = "user")
    private List<IncidentCase> incidentCase;
}
