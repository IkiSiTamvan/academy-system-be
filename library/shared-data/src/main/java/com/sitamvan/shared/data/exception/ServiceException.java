package com.sitamvan.shared.data.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatus status;

    private ServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ServiceException notFound(String message) {
        return new ServiceException(HttpStatus.NOT_FOUND, message);
    }

    public static ServiceException duplicate(String message) {
        return new ServiceException(HttpStatus.CONFLICT, message);
    }

    public static ServiceException badRequest(String message) {
        return new ServiceException(HttpStatus.BAD_REQUEST, message);
    }

    public static ServiceException unauthorized(String message) {
        return new ServiceException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ServiceException forbidden(String message) {
        return new ServiceException(HttpStatus.FORBIDDEN, message);
    }

    public static ServiceException internal(String message) {
        return new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}

