package com.sitamvan.shared.data.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RowStatusConverter implements AttributeConverter<RowStatus, Byte> {

    @Override
    public Byte convertToDatabaseColumn(RowStatus status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public RowStatus convertToEntityAttribute(Byte dbData) {
        return dbData != null ? RowStatus.fromCode(dbData) : null;
    }
}
