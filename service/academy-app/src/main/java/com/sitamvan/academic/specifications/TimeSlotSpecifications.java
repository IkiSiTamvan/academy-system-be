package com.sitamvan.academic.specifications;

import com.sitamvan.academic.model.TimeSlot;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class TimeSlotSpecifications {
    private TimeSlotSpecifications() { }
    public static Specification<TimeSlot> forSchool(Long schoolId) {
        return (root, query, cb) -> cb.and(cb.equal(root.get("school").get("id"), schoolId),
                cb.equal(root.get("academicYear").get("school").get("id"), schoolId),
                cb.equal(root.get("academicYear").get("rowStatus"), RowStatus.ACTIVE),
                cb.equal(root.get("rowStatus"), RowStatus.ACTIVE));
    }
    public static Specification<TimeSlot> forYear(Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get("academicYear").get("id"), id);
    }
    public static Specification<TimeSlot> forDay(Short day) {
        return day == null ? null : (root, query, cb) -> cb.equal(root.get("dayOfWeek"), day);
    }
    public static Specification<TimeSlot> search(String value) {
        if (value == null || value.isBlank()) return null;
        String term = "%" + value.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("label")), term, '\\');
    }
}
