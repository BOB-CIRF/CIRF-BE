package com.cirf.dashboard.domain.collect.dto.response;

import com.cirf.dashboard.domain.collect.dto.ProgressEvent;

public record ProgressResponse(
        String revision,
        String jobId,
        Integer collectId,
        Integer completed,
        Integer pending,
        Integer process,
        Integer fail,
        Integer totalJob,
        boolean terminal,
        String updatedAt
){
    public static ProgressResponse from(ProgressEvent event) {
        return new ProgressResponse(
                event.getRevision(),
                event.getJobId(),
                event.getCollectId(),
                event.getCompleted() != null ? event.getCompleted() : 0,
                event.getPending() != null ? event.getPending() : 0,
                event.getProcess() != null ? event.getProcess() : 0,
                event.getFail() != null ? event.getFail() : 0,
                event.getTotalJob() != null ? event.getTotalJob() : 0,
                event.isTerminal(),
                event.getUpdatedAt()
        );
    }
}
