package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestClassGroup;
import com.sitamvan.academic.dto.response.ResponseClassGroup;
import com.sitamvan.academic.model.ClassGroup;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClassGroupMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeLevel", source = "gradeLevel")
    @Mapping(target = "cohortYear", source = "cohortYear")
    @Mapping(target = "section", source = "section")
    @Mapping(target = "capacity", source = "capacity")
    void update(RequestClassGroup request, @MappingTarget ClassGroup entity);

    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "academicYearName", source = "academicYear.name")
    @Mapping(target = "majorId", source = "major.id")
    @Mapping(target = "majorName", source = "major.name")
    ResponseClassGroup toResponse(ClassGroup entity);
}
