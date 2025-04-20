package com.mercor.assignment.scd.domain.paymentlineitem.specification;

import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import jakarta.persistence.criteria.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentLineItemSpecification {

    /**
     * Creates a specification to find the latest version of each payment line item
     * that is associated with a specific contractor within a time period
     *
     * @param contractorId the contractor ID
     * @param startTime the start time of the period
     * @param endTime the end time of the period
     * @return the specification
     */
    public static Specification<PaymentLineItem> forContractorInTimePeriod(String contractorId, Long startTime, Long endTime) {
        return (root, query, criteriaBuilder) -> {
            // This handles getting latest versions when used with distinct
          assert query != null;
          if (isCountQuery(query)) {
                return null;
            }

            // Create subquery to find job UIDs belonging to the contractor
            Subquery<String> jobUidsSubquery = query.subquery(String.class);
            Root<Job> jobRoot = jobUidsSubquery.from(Job.class);
            jobUidsSubquery.select(jobRoot.get("uid"))
                .where(
                    criteriaBuilder.equal(jobRoot.get("contractorId"), contractorId),
                    isLatestVersion(jobRoot, criteriaBuilder, jobUidsSubquery)
                );

            // Create subquery to find timelog UIDs within the time period
            Subquery<String> timelogUidsSubquery = query.subquery(String.class);
            Root<Timelog> timelogRoot = timelogUidsSubquery.from(Timelog.class);
            timelogUidsSubquery.select(timelogRoot.get("uid"))
                .where(
                    criteriaBuilder.greaterThanOrEqualTo(timelogRoot.get("timeStart"), startTime),
                    criteriaBuilder.lessThanOrEqualTo(timelogRoot.get("timeEnd"), endTime),
                    isLatestVersion(timelogRoot, criteriaBuilder, timelogUidsSubquery)
                );

            // Main query to find payment line items that match the subqueries
            // and are the latest version
            Predicate paymentLineItemPredicate = criteriaBuilder.and(
                root.get("jobUid").in(jobUidsSubquery),
                root.get("timelogUid").in(timelogUidsSubquery),
                isLatestVersion(root, criteriaBuilder, query)
            );

            // Handle distinct if necessary
            if (!query.getResultType().equals(Long.class) && !query.getResultType().equals(long.class)) {
                query.distinct(true);
            }

            return paymentLineItemPredicate;
        };
    }

    /**
     * Creates a predicate to ensure we're getting the latest version of an entity
     *
     * @param root the root entity
     * @param criteriaBuilder the criteria builder
     * @param query the parent query
     * @return the predicate for latest version
     */
    private static <T> Predicate isLatestVersion(Root<T> root, CriteriaBuilder criteriaBuilder, AbstractQuery<?> query) {
        Subquery<Integer> maxVersionSubquery = query.subquery(Integer.class);
        // Use the raw type and cast to avoid generic type capture issues
        Root<?> subRoot = maxVersionSubquery.from(root.getJavaType());

        maxVersionSubquery.select(criteriaBuilder.max(subRoot.get("version")))
            .where(criteriaBuilder.equal(subRoot.get("id"), root.get("id")));

        return criteriaBuilder.equal(root.get("version"), maxVersionSubquery);
    }

    /**
     * Checks if the current query is a count query
     *
     * @param query the query
     * @return true if it's a count query, false otherwise
     */
    private static boolean isCountQuery(CriteriaQuery<?> query) {
        return query.getResultType().equals(Long.class) || query.getResultType().equals(long.class);
    }
}