package com.cirf.dashboard.domain.analysis.repository.awsNativeLog;

import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEventRepository extends ElasticsearchRepository<LogEvent, String>, LogEventRepositoryCustom {

}

