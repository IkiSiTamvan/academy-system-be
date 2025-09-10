package com.sitamvan.shared.data.util;

import lombok.Getter;

@Getter
public enum RowStatus {
    ACTIVE((byte) 1),
    DELETED((byte) 0);

    private final byte code;

    RowStatus(byte code) {
        this.code = code;
    }

    public static RowStatus fromCode(byte code) {
        for (RowStatus status : RowStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RowStatus code: " + code);
    }
}
