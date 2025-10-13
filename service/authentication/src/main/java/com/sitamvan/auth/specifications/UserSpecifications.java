package com.sitamvan.auth.specifications;

import com.sitamvan.auth.model.Role;
import com.sitamvan.auth.model.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
            Join<User, Role> roles = root.join("roles", JoinType.LEFT);
            cq.distinct(true);
            return cb.equal(cb.lower(roles.get("name")), rn);
        };
    }
}
