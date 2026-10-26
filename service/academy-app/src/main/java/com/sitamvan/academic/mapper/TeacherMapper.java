package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestTeacher;
import com.sitamvan.academic.dto.response.ResponseTeacher;
import com.sitamvan.academic.model.Teacher;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TeacherMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "employeeCode", source = "employeeCode")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "external", source = "external")
    void update(RequestTeacher request, @MappingTarget Teacher entity);

    ResponseTeacher toResponse(Teacher entity);
}
