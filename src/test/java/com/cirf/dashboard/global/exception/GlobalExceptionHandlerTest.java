package com.cirf.dashboard.global.exception;

import com.cirf.dashboard.global.common.dto.ExceptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    private BaseException testBaseException;

    @BeforeEach
    void setUp() {
        testBaseException = new BaseException(HttpStatus.BAD_REQUEST, "Test error message") {};
    }

    @Test
    @DisplayName("BaseException을 처리하고 적절한 응답을 반환한다")
    void handleException_BaseException_ReturnsCorrectResponse() {
        // when
        ResponseEntity<ExceptionResponse> response = exceptionHandler.handleException(testBaseException);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Test error message");
    }

    @Test
    @DisplayName("MissingRequestHeaderException을 처리한다")
    void handleMissingHeader_ReturnsUnauthorized() {
        // given
        MissingRequestHeaderException exception = new MissingRequestHeaderException(
                "Authorization",
                null
        );

        // when
        ResponseEntity<ExceptionResponse> response = exceptionHandler.handleMissingHeader(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("로그인이 필요합니다.");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException을 처리한다")
    void handleValidationException_ReturnsValidationErrors() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "필수 필드입니다");
        
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null,
                bindingResult
        );

        // when
        ResponseEntity<ExceptionResponse> response = exceptionHandler.handleValidationException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("필수 필드입니다");
    }

    @Test
    @DisplayName("다양한 HTTP 상태 코드를 가진 BaseException을 처리한다")
    void handleException_DifferentStatusCodes_ReturnsCorrectStatus() {
        // given
        BaseException notFoundException = new BaseException(HttpStatus.NOT_FOUND, "Not found") {};
        BaseException forbiddenException = new BaseException(HttpStatus.FORBIDDEN, "Forbidden") {};
        BaseException internalErrorException = new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error") {};

        // when
        ResponseEntity<ExceptionResponse> notFoundResponse = exceptionHandler.handleException(notFoundException);
        ResponseEntity<ExceptionResponse> forbiddenResponse = exceptionHandler.handleException(forbiddenException);
        ResponseEntity<ExceptionResponse> internalErrorResponse = exceptionHandler.handleException(internalErrorException);

        // then
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(forbiddenResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(internalErrorResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("검증 예외의 첫 번째 에러 메시지를 반환한다")
    void handleValidationException_MultipleErrors_ReturnsFirstError() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError firstError = new FieldError("object", "field1", "첫 번째 에러");
        FieldError secondError = new FieldError("object", "field2", "두 번째 에러");
        
        when(bindingResult.getAllErrors()).thenReturn(java.util.Arrays.asList(firstError, secondError));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null,
                bindingResult
        );

        // when
        ResponseEntity<ExceptionResponse> response = exceptionHandler.handleValidationException(exception);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("첫 번째 에러");
    }
}