package com.cirf.dashboard.domain.cases.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class SendOnboardingEmailRequest {

    @NotEmpty(message = "최소 하나의 이메일은 필수입니다")
    private List<@Email(message = "유효한 이메일 형식이어야 합니다") String> email;

    @NotBlank(message = "계정 ID는 필수입니다")
    private String accountId;

    public List<String> getEmail() {
        return email;
    }

    public String getAccountId() {
        return accountId;
    }
}