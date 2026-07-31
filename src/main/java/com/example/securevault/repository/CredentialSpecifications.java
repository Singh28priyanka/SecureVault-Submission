package com.example.securevault.repository;

import com.example.securevault.entity.Credential;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CredentialSpecifications {

    private CredentialSpecifications() {
    }

    public static Specification<Credential> ownedBy(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Credential> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Credential> deletedOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("deleted"));
    }

    public static Specification<Credential> categoryEquals(String category) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(category)) {
                return cb.conjunction();
            }
            return cb.equal(
                    cb.lower(root.get("category").get("name")),
                    category.trim().toLowerCase()
            );
        };
    }

    public static Specification<Credential> titleContains(String title) {
        return containsIgnoreCase("title", title);
    }

    public static Specification<Credential> usernameContains(String username) {
        return containsIgnoreCase("username", username);
    }

    public static Specification<Credential> websiteContains(String website) {
        return containsIgnoreCase("websiteUrl", website);
    }

    public static Specification<Credential> withFilters(Long userId,
                                                        String category,
                                                        String title,
                                                        String username,
                                                        String website) {
        return ownedBy(userId)
                .and(notDeleted())
                .and(categoryEquals(category))
                .and(titleContains(title))
                .and(usernameContains(username))
                .and(websiteContains(website));
    }

    public static Specification<Credential> trashForUser(Long userId) {
        return ownedBy(userId).and(deletedOnly());
    }

    private static Specification<Credential> containsIgnoreCase(String field, String value) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(value)) {
                return cb.conjunction();
            }
            return cb.like(
                    cb.lower(root.get(field)),
                    "%" + value.trim().toLowerCase() + "%"
            );
        };
    }
}
