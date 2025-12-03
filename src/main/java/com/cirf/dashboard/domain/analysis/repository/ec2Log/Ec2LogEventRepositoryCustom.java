package com.cirf.dashboard.domain.analysis.repository.ec2Log;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2LogQueryRequest;
import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;

public interface Ec2LogEventRepositoryCustom {

    SliceWithSort<Ec2LogEvent> queryEc2LogEvents(String tenantId, Ec2LogQueryRequest ec2LogQueryRequest);
}
