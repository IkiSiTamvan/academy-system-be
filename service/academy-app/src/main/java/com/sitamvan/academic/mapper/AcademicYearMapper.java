package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestAcademicYear;
import com.sitamvan.academic.dto.response.ResponseAcademicYear;
import com.sitamvan.academic.model.AcademicYear;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AcademicYearMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "startsOn", source = "startsOn")
    @Mapping(target = "endsOn", source = "endsOn")
    @Mapping(target = "active", source = "active")
    void update(RequestAcademicYear request, @MappingTarget AcademicYear entity);

    ResponseAcademicYear toResponse(AcademicYear entity);
}
