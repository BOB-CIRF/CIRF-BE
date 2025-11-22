package com.cirf.dashboard.domain.collect.dto;

import com.cirf.dashboard.domain.collect.dto.request.WebhookRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEvent {
    private String revision;        // 리비전 (버전 관리용)
    private String jobId;           // Job ID
    private Integer collectId;       // Collect ID (캐시 키로 사용)
    private Integer completed;       // 완료된 작업 수
    private Integer pending;         // 대기 중인 작업 수
    private Integer process;         // 진행 중인 작업 수
    private Integer fail;            // 실패한 작업 수
    private Integer totalJob;        // 전체 작업 수
    private boolean terminal;        // 종료 여부
    private String updatedAt;       // 업데이트 시간

    public static ProgressEvent toEntity(WebhookRequest request){
        return ProgressEvent.builder()
                .revision(request.revision())
                .jobId(request.jobId())
                .collectId(request.collectId())
                .completed(request.completed())
                .process(request.process())
                .pending(request.pending())
                .fail(request.fail())
                .totalJob(request.totalJob())
                .terminal(request.terminal())
                .updatedAt(request.updatedAt())
                .build();
    }
}
