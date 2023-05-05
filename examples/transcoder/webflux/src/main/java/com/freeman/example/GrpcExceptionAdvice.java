package com.freeman.example;

import com.freemanan.starter.grpc.extensions.transcoderhttp.util.GrpcUtil;
import io.grpc.StatusRuntimeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Freeman
 */
@ControllerAdvice
public class GrpcExceptionAdvice {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleStatusRuntimeException(StatusRuntimeException sre) {
        HttpStatus httpStatus = GrpcUtil.toHttpCode(sre.getStatus());
        return ResponseEntity.status(httpStatus).body(new ErrorResponse(httpStatus.value(), sre.getMessage(), null));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ErrorResponse {
        private int code;
        private String message;
        private Object data;
    }
}
