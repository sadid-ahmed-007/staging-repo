package com.eduauth.repository.specification;

import com.eduauth.model.ActivityLog;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogSpecification {

    public static Specification<ActivityLog> withFilters(String filter, String type) {
        return (Root<ActivityLog> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Time Filter
            if (filter != null && !filter.isBlank() && !"all".equalsIgnoreCase(filter)) {
                if ("today".equalsIgnoreCase(filter)) {
                    LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
                } else if ("this_week".equalsIgnoreCase(filter)) {
                    LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfWeek));
                } else if ("this_month".equalsIgnoreCase(filter)) {
                    LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfMonth));
                }
            }

            // 2. Type Filter
            if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type)) {
                String t = type.toLowerCase().trim();
                if ("user_action".equals(t)) {
                    Predicate pAction = cb.or(
                            cb.like(cb.lower(root.get("action")), "user_%"),
                            cb.like(cb.lower(root.get("action")), "profile_%"),
                            cb.like(cb.lower(root.get("action")), "auth_%"),
                            cb.like(cb.lower(root.get("action")), "login%"),
                            cb.like(cb.lower(root.get("action")), "%suspended%"),
                            cb.like(cb.lower(root.get("action")), "%approved%")
                    );
                    Predicate pEntity = cb.or(
                            cb.like(cb.lower(root.get("entityType")), "%user%"),
                            cb.like(cb.lower(root.get("entityType")), "%profile%")
                    );
                    predicates.add(cb.or(pAction, pEntity));
                } else if ("certificate".equals(t)) {
                    Predicate pAction = cb.or(
                            cb.like(cb.lower(root.get("action")), "cert_%"),
                            cb.like(cb.lower(root.get("action")), "certificate_%")
                    );
                    Predicate pEntity = cb.like(cb.lower(root.get("entityType")), "%certificate%");
                    predicates.add(cb.or(pAction, pEntity));
                } else if ("enrollment".equals(t)) {
                    Predicate pAction = cb.or(
                            cb.like(cb.lower(root.get("action")), "enrollment_%"),
                            cb.like(cb.lower(root.get("action")), "student_%")
                    );
                    Predicate pEntity = cb.like(cb.lower(root.get("entityType")), "%enrollment%");
                    predicates.add(cb.or(pAction, pEntity));
                } else if ("system".equals(t)) {
                    Predicate pAction = cb.or(
                            cb.like(cb.lower(root.get("action")), "system_%"),
                            cb.like(cb.lower(root.get("action")), "access_%")
                    );
                    Predicate pEntity = cb.isNull(root.get("userId"));
                    predicates.add(cb.or(pAction, pEntity));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
