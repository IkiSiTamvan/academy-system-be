package com.sitamvan.academic.mapper;

import com.sitamvan.academic.dto.request.RequestRoom;
import com.sitamvan.academic.dto.response.ResponseRoom;
import com.sitamvan.academic.model.Room;
import com.sitamvan.academic.model.Facility;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "roomType", source = "roomType")
    @Mapping(target = "capacity", source = "capacity")
    void update(RequestRoom request, @MappingTarget Room entity);

    default ResponseRoom toResponse(Room room, List<Facility> facilities) {
        return new ResponseRoom(room.getId(), room.getCode(), room.getName(), room.getRoomType(), room.getCapacity(),
                facilities.stream().map(Facility::getId).toList(), facilities.stream().map(Facility::getName).toList(),
                room.getCreatedAt(), room.getModifiedAt());
    }
}
