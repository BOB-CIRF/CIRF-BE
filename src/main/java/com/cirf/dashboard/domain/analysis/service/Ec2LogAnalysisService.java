package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.dto.request.Ec2LogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogResponse;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class Ec2LogAnalysisService {

    public Slice<Ec2LogResponse> queryEc2Log(
            long userId,
            long caseId,
            String accountId,
            String instanceId,
            Ec2LogQueryRequest queryRequest
    ) {

    }
}
