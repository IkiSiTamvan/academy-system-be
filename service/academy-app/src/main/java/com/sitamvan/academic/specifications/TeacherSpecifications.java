package com.sitamvan.academic.specifications;

import com.sitamvan.academic.model.Teacher;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class TeacherSpecifications {
    private TeacherSpecifications() { }

    public static Specification<Teacher> forSchool(Long schoolId) {
        return (root, query, cb) -> cb.and(cb.equal(root.get("school").get("id"), schoolId),
                cb.equal(root.get("rowStatus"), RowStatus.ACTIVE));
    }

    public static Specification<Teacher> search(String search) {
        if (search == null || search.isBlank()) return null;
        String term = "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("employeeCode")), term, '\\'),
                cb.like(cb.lower(root.get("name")), term, '\\'),
                cb.like(cb.lower(root.get("email")), term, '\\'),
                cb.like(cb.lower(root.get("phone")), term, '\\'));
    }

    public static Specification<Teacher> active(Boolean active) {
        return active == null ? null : (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
