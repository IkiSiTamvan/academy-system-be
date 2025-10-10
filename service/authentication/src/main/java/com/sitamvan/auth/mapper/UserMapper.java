package com.sitamvan.auth.mapper;

import com.sitamvan.auth.dto.request.RequestUserCreate;
import com.sitamvan.auth.dto.request.RequestUserUpdate;
import com.sitamvan.auth.dto.response.ResponseUser;
import com.sitamvan.auth.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "enabled", source = "isActive")
    User toEntityFromCreate(RequestUserCreate dto);

    @Mapping(target = "isActive", source = "enabled")
    ResponseUser toResponse(User entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "enabled", source = "isActive")
    void toEntityFromUpdate(RequestUserUpdate dto, @MappingTarget User entity);
}
