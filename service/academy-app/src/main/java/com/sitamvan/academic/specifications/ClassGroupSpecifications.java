package com.sitamvan.academic.specifications;

import com.sitamvan.academic.model.ClassGroup;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class ClassGroupSpecifications {
    private ClassGroupSpecifications() { }
    public static Specification<ClassGroup> forSchool(Long schoolId) {
        return (root, query, cb) -> cb.and(cb.equal(root.get("school").get("id"), schoolId),
                cb.equal(root.get("rowStatus"), RowStatus.ACTIVE),
                cb.equal(root.get("academicYear").get("rowStatus"), RowStatus.ACTIVE),
                cb.equal(root.get("major").get("rowStatus"), RowStatus.ACTIVE));
    }
    public static Specification<ClassGroup> forYear(Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get("academicYear").get("id"), id);
    }
    public static Specification<ClassGroup> forMajor(Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get("major").get("id"), id);
    }
    public static Specification<ClassGroup> search(String q) {
        if (q == null || q.isBlank()) return null;
        String term = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("code")), term, '\\'),
                cb.like(cb.lower(root.get("name")), term, '\\'));
    }
}
