package com.sitamvan.shared.data.util;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class ASoftDeletable extends AAuditable {

    @Column(name = "row_status", nullable = false)
    private RowStatus rowStatus = RowStatus.ACTIVE;
}

