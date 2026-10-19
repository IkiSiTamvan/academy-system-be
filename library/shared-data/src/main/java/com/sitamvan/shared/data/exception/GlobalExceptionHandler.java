package com.sitamvan.shared.data.exception;

import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<BaseResponse<?>> handleMyCustomException(ServiceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ResponseBuilder.error(ex.getStatus(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid input");

        return ResponseEntity.badRequest().body(ResponseBuilder.error(HttpStatus.BAD_REQUEST, errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
    }
}

