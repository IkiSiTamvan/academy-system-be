package com.sitamvan.shared.data.util;

import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.base.ResponseInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;

import java.util.*;

public final class ResponseBuilder {

    private ResponseBuilder() {
        // Utility class; prevent instantiation
    }

    public static <T> BaseResponse<T> ok(T result, String message) {
        return new BaseResponse<>(
                result,
                new ResponseInfo(HttpStatus.OK.value(), message, true),
                null
        );
    }

    public static <T> BaseResponse<List<T>> ok(List<T> result, String message) {
        return new BaseResponse<>(
                result,
                new ResponseInfo(HttpStatus.OK.value(), message, true),
                null
        );
    }

    public static <T> BaseResponse<List<T>> ok(Page<T> result, String message, Map<String, Object> metaData) {
        if (Objects.isNull(metaData)) {
            metaData = new HashMap<>();
        }
        metaData.put("pageCount", result.getTotalPages());
        metaData.put("totalData", result.getTotalElements());

        return new BaseResponse<>(
                result.getContent(),
                new ResponseInfo(HttpStatus.OK.value(), message, true),
                metaData
        );
    }

    public static <T> BaseResponse<List<T>> ok(Slice<T> result, String message,
                                               Map<String, Object> metaData,
                                               Integer totalPages,
                                               Integer totalElements) {
        if (Objects.isNull(metaData)) {
            metaData = new HashMap<>();
        }
        metaData.put("pageCount", totalPages);
        metaData.put("totalData", totalElements);

        return new BaseResponse<>(
                result.getContent(),
                new ResponseInfo(HttpStatus.OK.value(), message, true),
                metaData
        );
    }

    public static <T> BaseResponse<T> error(HttpStatus status, String message) {
        return new BaseResponse<>(
                null,
                new ResponseInfo(status.value(), message, false),
                null
        );
    }
}
