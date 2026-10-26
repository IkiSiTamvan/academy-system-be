package com.sitamvan.academic.specifications;

import com.sitamvan.academic.model.Room;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class RoomSpecifications {
    private RoomSpecifications() { }
    public static Specification<Room> forSchool(Long schoolId) {
        return (root, query, cb) -> cb.and(cb.equal(root.get("school").get("id"), schoolId), cb.equal(root.get("rowStatus"), RowStatus.ACTIVE));
    }
    public static Specification<Room> search(String query) {
        if (query == null || query.isBlank()) return null;
        String term = "%" + query.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, criteria, cb) -> cb.or(cb.like(cb.lower(root.get("code")), term, '\\'), cb.like(cb.lower(root.get("name")), term, '\\'), cb.like(cb.lower(root.get("roomType")), term, '\\'));
    }
}
