package com.cirf.dashboard.domain.scan.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScanResultsRequest(
        @NotBlank(message = "accountId는 필수입니다.")
        String accountId,

        @NotBlank(message = "region은 필수입니다.")
        String region,

        @NotNull(message = "pageNumber는 필수입니다.")
        @Min(value = 0, message = "pageNumber는 0 이상이어야 합니다.")
        Integer pageNumber,

        @NotNull(message = "pageSize는 필수입니다.")
        @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
        Integer pageSize
) {
}
