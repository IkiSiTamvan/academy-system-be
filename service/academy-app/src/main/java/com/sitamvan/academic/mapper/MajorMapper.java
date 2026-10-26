package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestMajor;
import com.sitamvan.academic.dto.response.ResponseMajor;
import com.sitamvan.academic.model.Major;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MajorMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "programName", source = "programName")
    @Mapping(target = "fieldName", source = "fieldName")
    void update(RequestMajor request, @MappingTarget Major entity);

    ResponseMajor toResponse(Major entity);
}
