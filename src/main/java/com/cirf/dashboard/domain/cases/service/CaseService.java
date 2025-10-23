package com.cirf.dashboard.domain.cases.service;

import com.cirf.dashboard.domain.cases.repository.AccountIdRepository;
import com.cirf.dashboard.domain.cases.repository.IncidentCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final AccountIdRepository accountIdRepository;
    private final IncidentCaseRepository incidentCaseRepository;
}
