package com.sitamvan.auth.specifications;

import com.sitamvan.auth.model.SchoolMembershipRole;
import com.sitamvan.auth.model.User;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> queryLike(String query) {
        if (query == null || query.isBlank()) return null;
        final String like = "%" + query.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), like),
                cb.like(cb.lower(root.get("email")), like)
        );
    }

    public static Specification<User> usernameLike(String username) {
        if (username == null || username.isBlank()) return null;
        final String like = "%" + username.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get("username")), like);
    }

    public static Specification<User> emailLike(String email) {
        if (email == null || email.isBlank()) return null;
        final String like = "%" + email.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get("email")), like);
    }

    public static Specification<User> enabledEquals(Boolean enabled) {
        if (enabled == null) return null;
        return (root, cq, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<User> idsIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return (root, cq, cb) -> root.get("id").in(ids);
    }

    public static Specification<User> roleEquals(String roleName) {
        if (roleName == null || roleName.isBlank()) return null;
        final String rn = roleName.trim().toLowerCase();
        return (root, cq, cb) -> {
            Subquery<Long> roleMatch = cq.subquery(Long.class);
            var assignment = roleMatch.from(SchoolMembershipRole.class);
            roleMatch.select(assignment.get("membership").get("user").get("id"))
                    .where(cb.equal(assignment.get("membership").get("user").get("id"), root.get("id")),
                            cb.equal(cb.lower(assignment.get("role").get("code")), rn));
            return cb.exists(roleMatch);
        };
    }
}
