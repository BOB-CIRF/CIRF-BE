package com.cirf.dashboard.domain.collect.dto.event;

import com.cirf.dashboard.domain.collect.entity.CollectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressStatusEvent {
    private Long progressId;
    private Integer completed;
    private Integer pending;
    private Integer process;
    private Integer fail;
    private Integer totalJob;
    private Boolean terminal;        // 전체 작업 완료 여부
    private String updatedAt;

    public static ProgressStatusEvent from(CollectStatus status) {
        return ProgressStatusEvent.builder()
                .progressId(status.getProgressId())
                .completed(status.getCompleted() != null ? status.getCompleted() : 0)
                .pending(status.getPending() != null ? status.getPending() : 0)
                .process(status.getProcess() != null ? status.getProcess() : 0)
                .fail(status.getFail() != null ? status.getFail() : 0)
                .totalJob(status.getTotalJob() != null ? status.getTotalJob() : 0)
                .terminal(isJobCompleted(status))
                .updatedAt(status.getUpdatedAt())
                .build();
    }

    private static boolean isJobCompleted(CollectStatus status) {
        int completed = status.getCompleted() != null ? status.getCompleted() : 0;
        int fail = status.getFail() != null ? status.getFail() : 0;
        int totalJob = status.getTotalJob() != null ? status.getTotalJob() : 0;

        return totalJob > 0 && (completed + fail) >= totalJob;
    }

    public boolean isTerminal() {
        return terminal != null && terminal;
    }
}
