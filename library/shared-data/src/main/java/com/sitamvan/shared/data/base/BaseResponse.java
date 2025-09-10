package com.sitamvan.shared.data.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

    private T result;
    private ResponseInfo response;
    private Map<String, Object> metaData;

    public static <T> BaseResponse<T> success(T result, String message) {
        return new BaseResponse<>(
                result,
                new ResponseInfo(200, message, true),
                null
        );
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(
                null,
                new ResponseInfo(code, message, false),
                null
        );
    }
}
