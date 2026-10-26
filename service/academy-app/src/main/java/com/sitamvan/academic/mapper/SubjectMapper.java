package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestSubject;
import com.sitamvan.academic.dto.response.ResponseSubject;
import com.sitamvan.academic.model.Subject;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SubjectMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "category", source = "category")
    void update(RequestSubject request, @MappingTarget Subject entity);

    ResponseSubject toResponse(Subject entity);
}
