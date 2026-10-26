package com.sitamvan.academic.specifications;

import com.sitamvan.academic.model.AcademicPeriod;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class AcademicPeriodSpecifications {
    private AcademicPeriodSpecifications() { }

    public static Specification<AcademicPeriod> forSchool(Long schoolId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("academicYear").get("school").get("id"), schoolId),
                cb.equal(root.get("academicYear").get("rowStatus"), RowStatus.ACTIVE),
                cb.equal(root.get("rowStatus"), RowStatus.ACTIVE));
    }

    public static Specification<AcademicPeriod> forYear(Long yearId) {
        return yearId == null ? null : (root, query, cb) -> cb.equal(root.get("academicYear").get("id"), yearId);
    }

    public static Specification<AcademicPeriod> search(String search) {
        if (search == null || search.isBlank()) return null;
        String term = "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), term, '\\'),
                cb.like(cb.lower(root.get("name")), term, '\\'));
    }
}
