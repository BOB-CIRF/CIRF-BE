package com.cirf.dashboard.domain.scan.dto.response.lists;

import com.cirf.dashboard.domain.scan.dto.response.PageableResponse;
import com.cirf.dashboard.domain.scan.dto.response.ScanResultsResponse;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record ScanResultsListResponse(
        List<ScanResultsResponse> enabledLogs,
        PageableResponse pageable
) {
    public static ScanResultsListResponse of(Slice<ScanResultsResponse> slice) {
        return ScanResultsListResponse.builder()
                .enabledLogs(slice.getContent())
                .pageable(PageableResponse.of(slice))
                .build();
    }
}
