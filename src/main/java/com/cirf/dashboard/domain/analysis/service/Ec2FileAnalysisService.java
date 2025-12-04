package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2FileStatQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2FileStatResponse;
import com.cirf.dashboard.domain.analysis.entity.Ec2FileStat;
import com.cirf.dashboard.domain.analysis.repository.ec2File.Ec2FileStatRepository;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Ec2FileAnalysisService {

    private final UserRepository userRepository;
    private final Ec2FileStatRepository ec2FileStatRepository;

    public SliceWithSort<Ec2FileStatResponse> getFileStatList(long userId, Ec2FileStatQueryRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        String tenantId = String.valueOf(user.getTenant().getId());

        // DynamoDB 쿼리 (size + 1로 조회하여 hasNext 확인)
        List<Ec2FileStat> items = ec2FileStatRepository.queryFileStats(
                tenantId,
                request.caseId(),
                request.accountId(),
                request.region(),
                request.instanceId(),
                request.keyword(),
                request.pageNumber(),
                request.pageSize()
        );

        // hasNext 확인 (size + 1로 조회했으므로)
        boolean hasNext = items.size() > request.pageSize();

        // 실제 반환할 데이터는 size만큼만
        List<Ec2FileStat> content = items.stream()
                .limit(request.pageSize())
                .toList();

        // Entity -> DTO 변환
        List<Ec2FileStatResponse> responses = content.stream()
                .map(item -> new Ec2FileStatResponse(
                        item.getInstanceId(),
                        item.getFile(),
                        item.getSize(),
                        item.getAccess(),
                        item.getAccessTime(),
                        item.getModifyTime(),
                        item.getChangeTime()
                ))
                .toList();


        long totalElements = 0;

        totalElements = ec2FileStatRepository.countFileStats(
                    tenantId,
                    request.caseId(),
                    request.accountId(),
                    request.region(),
                    request.instanceId(),
                    request.keyword());
        log.info("Total elements count: {}", totalElements);


        Slice<Ec2FileStatResponse> slice = new SliceImpl<>(
                responses,
                PageRequest.of(request.pageNumber(), request.pageSize()),
                hasNext
        );

        return SliceWithSort.of(slice, null, totalElements);
    }
}
