package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestFacility;
import com.sitamvan.academic.dto.response.ResponseFacility;
import com.sitamvan.academic.model.Facility;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FacilityMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    void update(RequestFacility request, @MappingTarget Facility entity);
    ResponseFacility toResponse(Facility entity);
}
