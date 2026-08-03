package com.smartqueue.common.exception;

import com.smartqueue.common.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base application exception carrying an {@link ErrorCode} for structured error responses.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detail = errorCode.getDefaultMessage();
    }

    public AppException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public AppException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
