package com.eduauth.repository.specification;

import com.eduauth.model.Institution;
import com.eduauth.model.Student;
import com.eduauth.model.User;
import com.eduauth.model.Verifier;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> withFilters(String status, String role, String search) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            // Status filter
            if (status != null && !status.equalsIgnoreCase("all")) {
                if (status.equalsIgnoreCase("pending")) {
                    predicate = cb.and(predicate, cb.isFalse(root.get("isApproved")));
                    predicate = cb.and(predicate, cb.isNotNull(root.get("emailVerifiedAt")));
                } else if (status.equalsIgnoreCase("approved")) {
                    predicate = cb.and(predicate, cb.isTrue(root.get("isApproved")));
                    predicate = cb.and(predicate, cb.isNull(root.get("suspendedAt")));
                } else if (status.equalsIgnoreCase("suspended")) {
                    predicate = cb.and(predicate, cb.isNotNull(root.get("suspendedAt")));
                }
            }

            // Role filter
            if (role != null && !role.equalsIgnoreCase("all")) {
                predicate = cb.and(predicate, cb.equal(root.get("role"), role.toLowerCase()));
            }

            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate emailPredicate = cb.like(cb.lower(root.get("email")), searchPattern);

                // Need left joins because the relationship might not exist yet
                Join<User, Student> studentJoin = root.join("student", JoinType.LEFT);
                Join<User, Institution> institutionJoin = root.join("institution", JoinType.LEFT);
                Join<User, Verifier> verifierJoin = root.join("verifier", JoinType.LEFT);

                Predicate firstNamePredicate = cb.like(cb.lower(studentJoin.get("firstName")), searchPattern);
                Predicate lastNamePredicate = cb.like(cb.lower(studentJoin.get("lastName")), searchPattern);
                Predicate instNamePredicate = cb.like(cb.lower(institutionJoin.get("name")), searchPattern);
                Predicate verifierNamePredicate = cb.like(cb.lower(verifierJoin.get("companyName")), searchPattern);

                Predicate profilePredicate = cb.or(
                        emailPredicate,
                        firstNamePredicate,
                        lastNamePredicate,
                        instNamePredicate,
                        verifierNamePredicate
                );

                predicate = cb.and(predicate, profilePredicate);
            }

            return predicate;
        };
    }
}
