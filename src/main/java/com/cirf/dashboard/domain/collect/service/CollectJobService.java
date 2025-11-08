package com.cirf.dashboard.domain.collect.service;

import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.collect.dto.response.JobDetailResponse;
import com.cirf.dashboard.domain.collect.dto.response.JobListResponse;
import com.cirf.dashboard.domain.collect.entity.CollectJob;
import com.cirf.dashboard.domain.collect.exception.ErrorMessage;
import com.cirf.dashboard.domain.collect.exception.NotFoundCollectJobException;
import com.cirf.dashboard.domain.collect.repository.CollectJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectJobService {

    private final CollectJobRepository collectJobRepository;
    private final UserRepository userRepository;

    public Slice<JobDetailResponse> getJobDetail(long userId, long collectId, int pageNumber, int pageSize) {
        validateUser(userId, collectId);

        List<CollectJob> jobs = collectJobRepository.findAllByCollectId(String.valueOf(collectId));

        if (jobs.isEmpty()) {
            log.warn("No jobs found for collectId: {}", collectId);
            throw new NotFoundCollectJobException(ErrorMessage.COLLECT_JOB_NOT_FOUND);
        }

        // CollectJob -> JobDetailResponse 변환
        List<JobDetailResponse> jobDetails = jobs.stream()
                .map(JobDetailResponse::from)
                .toList();

        // 페이지네이션 적용
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), jobDetails.size());

        List<JobDetailResponse> pagedContent = jobDetails.subList(start, end);
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
