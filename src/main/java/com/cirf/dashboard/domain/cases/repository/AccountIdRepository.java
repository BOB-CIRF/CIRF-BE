package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.AccountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountIdRepository extends JpaRepository<AccountId, Long> {
}
