package com.sitamvan.academic.specifications;

import com.sitamvan.academic.model.Facility;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class FacilitySpecifications {
    private FacilitySpecifications() { }

    public static Specification<Facility> forSchool(Long schoolId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("school").get("id"), schoolId),
                cb.equal(root.get("rowStatus"), RowStatus.ACTIVE));
    }

    public static Specification<Facility> search(String search) {
        if (search == null || search.isBlank()) return null;
        String term = "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), term, '\\'),
                cb.like(cb.lower(root.get("name")), term, '\\'));
    }
}
