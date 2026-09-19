package com.eduauth.repository.specification;

import com.eduauth.model.ProfileChangeRequest;
import com.eduauth.model.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ProfileChangeRequestSpecification {

    public static Specification<ProfileChangeRequest> withFilters(String status, String role, String fieldName) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status.toLowerCase()));
            }

            if (role != null && !role.isBlank() && !role.equalsIgnoreCase("all")) {
                Join<ProfileChangeRequest, User> userJoin = root.join("user", JoinType.LEFT);
                predicate = cb.and(predicate, cb.equal(userJoin.get("role"), role.toLowerCase()));
            }

            if (fieldName != null && !fieldName.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("fieldName"), fieldName));
            }

            // Apply custom sort only on result queries, not count queries
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
                    query.orderBy(cb.desc(root.get("createdAt")));
                } else {
                    query.orderBy(
                        cb.asc(cb.selectCase()
                            .when(cb.equal(root.get("status"), "pending"), 0)
                            .otherwise(1)),
                        cb.desc(root.get("createdAt"))
                    );
                }
            }

            return predicate;
        };
    }
}
