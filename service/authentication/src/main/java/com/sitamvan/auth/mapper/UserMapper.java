package com.sitamvan.auth.mapper;

import com.sitamvan.auth.dto.request.CreateUserRequest;
import com.sitamvan.auth.dto.request.UpdateUserRequest;
import com.sitamvan.auth.dto.response.ResponseUser;
import com.sitamvan.auth.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "enabled", source = "isActive")
    User toEntityFromCreate(CreateUserRequest dto);

    @Mapping(target = "isActive", source = "enabled")
    ResponseUser toResponse(User entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "enabled", source = "isActive")
    void toEntityFromUpdate(UpdateUserRequest dto, @MappingTarget User entity);
}
