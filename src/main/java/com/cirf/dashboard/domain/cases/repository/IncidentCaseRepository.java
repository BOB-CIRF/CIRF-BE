package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.IncidentCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentCaseRepository extends JpaRepository<IncidentCase, Long> {
}
