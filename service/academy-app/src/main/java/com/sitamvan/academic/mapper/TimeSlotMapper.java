package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestTimeSlot;
import com.sitamvan.academic.dto.response.ResponseTimeSlot;
import com.sitamvan.academic.model.TimeSlot;
import org.mapstruct.*;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface TimeSlotMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "dayOfWeek", source = "dayOfWeek")
    @Mapping(target = "sequenceNumber", source = "sequenceNumber")
    @Mapping(target = "label", source = "label")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "kind", source = "kind")
    void update(RequestTimeSlot request, @MappingTarget TimeSlot entity);

    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "academicYearName", source = "academicYear.name")
    @Mapping(target = "dayName", expression = "java(dayName(entity.getDayOfWeek()))")
    @Mapping(target = "kindLabel", expression = "java(kindLabel(entity.getKind()))")
    ResponseTimeSlot toResponse(TimeSlot entity);

    default String dayName(short day) { return DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID")); }
    default String kindLabel(TimeSlot.Kind kind) {
        return switch (kind) { case LESSON -> "Pelajaran"; case BREAK -> "Istirahat"; case ACTIVITY -> "Kegiatan"; case OTHER -> "Lainnya"; };
    }
}
