package com.cirf.dashboard.domain.analysis.repository.ec2Log;

import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Ec2LogEventRepository extends ElasticsearchRepository<Ec2LogEvent, String>, Ec2LogEventRepositoryCustom {

}
