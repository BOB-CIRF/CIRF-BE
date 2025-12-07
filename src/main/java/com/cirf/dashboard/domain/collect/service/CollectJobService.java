package com.cirf.dashboard.domain.collect.service;

import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.collect.dto.response.AwsNativeJobDetailResponse;
import com.cirf.dashboard.domain.collect.dto.response.Ec2JobResponse;
import com.cirf.dashboard.domain.collect.entity.CollectEc2Job;
import com.cirf.dashboard.domain.collect.entity.CollectJob;
import com.cirf.dashboard.domain.collect.exception.ErrorMessage;
import com.cirf.dashboard.domain.collect.exception.NotFoundCollectJobException;
import com.cirf.dashboard.domain.collect.repository.CollectEc2JobRepository;
import com.cirf.dashboard.domain.collect.repository.CollectJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectJobService {

    private final CollectJobRepository collectJobRepository;
    private final CollectEc2JobRepository collectEc2JobRepository;
    private final UserRepository userRepository;

    public Slice<AwsNativeJobDetailResponse> getJobDetail(long userId, long collectId, int pageNumber, int pageSize) {
        validateUser(userId, collectId);

        List<CollectJob> jobs = collectJobRepository.findAllByCollectId(String.valueOf(collectId));

        if (jobs.isEmpty()) {
            log.warn("No jobs found for collectId: {}", collectId);
            throw new NotFoundCollectJobException(ErrorMessage.COLLECT_JOB_NOT_FOUND);
        }

        // CollectJob -> JobDetailResponse 변환
        List<AwsNativeJobDetailResponse> jobDetails = jobs.stream()
                .map(AwsNativeJobDetailResponse::from)
                .toList();

        // 페이지네이션 적용
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), jobDetails.size());

        List<AwsNativeJobDetailResponse> pagedContent = jobDetails.subList(start, end);
        boolean hasNext = end < jobDetails.size();

        return new SliceImpl<>(pagedContent, pageable, hasNext);
    }

    public Slice<Ec2JobResponse> getEc2JobDetail(long userId, long collectId, int pageNumber, int pageSize) {
        validateUser(userId, collectId);

        List<CollectEc2Job> jobs = collectEc2JobRepository.findAllByCollectId(collectId);

        if (jobs.isEmpty()) {
            log.warn("No jobs found for collectId: {}", collectId);
            throw new NotFoundCollectJobException(ErrorMessage.COLLECT_JOB_NOT_FOUND);
        }

        // CollectJob -> JobDetailResponse 변환
        List<Ec2JobResponse> jobDetails = jobs.stream().map(Ec2JobResponse::from).collect(Collectors.toList());

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), jobDetails.size());

        List<Ec2JobResponse> pagedContent = jobDetails.subList(start, end);
        boolean hasNext = end < jobDetails.size();

        return new SliceImpl<>(pagedContent, pageable, hasNext);
    }

    public void validateUser(long userId, long collectId) {
        // userId 검증
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }

}
