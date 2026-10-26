package com.sitamvan.academic.specifications;

import com.sitamvan.academic.model.Major;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.Locale;

public final class MajorSpecifications {
    private MajorSpecifications() { }

    public static Specification<Major> forSchool(Long schoolId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("school").get("id"), schoolId),
                cb.equal(root.get("rowStatus"), RowStatus.ACTIVE));
    }

    public static Specification<Major> search(String search) {
        if (search == null || search.isBlank()) return null;
        String term = "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), term, '\\'),
                cb.like(cb.lower(root.get("name")), term, '\\'),
                cb.like(cb.lower(root.get("programName")), term, '\\'),
                cb.like(cb.lower(root.get("fieldName")), term, '\\'));
    }
}
