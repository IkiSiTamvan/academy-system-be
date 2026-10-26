package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestAcademicPeriod;
import com.sitamvan.academic.dto.response.ResponseAcademicPeriod;
import com.sitamvan.academic.model.AcademicPeriod;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AcademicPeriodMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "startsOn", source = "startsOn")
    @Mapping(target = "endsOn", source = "endsOn")
    void update(RequestAcademicPeriod request, @MappingTarget AcademicPeriod entity);

    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "academicYearName", source = "academicYear.name")
    ResponseAcademicPeriod toResponse(AcademicPeriod entity);
}
