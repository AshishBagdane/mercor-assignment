package com.mercor.assignment.scd.domain.core.validation;

import com.mercor.assignment.scd.common.validation.Validators;
import com.mercor.assignment.scd.common.validation.Validators.BigDecimalValidators;
import com.mercor.assignment.scd.common.validation.Validators.StringValidators;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Domain-specific validators for the SCD model entities
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SCDValidators {

    /**
     * Common validators for SCD fields
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SCDCommonValidators {
        // ID validators
        public static final Validators.Validator<String> validId = StringValidators.notEmpty
            .and(value -> value.startsWith("job_") || value.startsWith("tl_") || value.startsWith("li_"));

        // UID validators
        public static final Validators.Validator<String> validUid = StringValidators.notEmpty
            .and(value -> value.contains("_uid_"));

        // Version validators
        public static final Validators.Validator<Integer> validVersion = value -> value != null && value > 0;
    }

    /**
     * Job-specific validators
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JobValidators {
        // Status validators
        public static final Validators.Validator<String> validStatus = StringValidators.notEmpty
            .and(value -> value.equals("active") || value.equals("extended") || value.equals("completed"));

        // Rate validators
        public static final Validators.Validator<BigDecimal> validRate = BigDecimalValidators.notNull
            .and(BigDecimalValidators.isPositive)
            .and(BigDecimalValidators.hasMaxScale(2))
            .and(BigDecimalValidators.lessThan(new BigDecimal("1000.00")));

        // Title validators
        public static final Validators.Validator<String> validTitle = StringValidators.notEmpty
            .and(StringValidators.hasMinLength(3))
            .and(StringValidators.hasMaxLength(100));

        // ID validators
        public static final Validators.Validator<String> validCompanyId = StringValidators.notEmpty
            .and(value -> value.startsWith("comp_"));

        public static final Validators.Validator<String> validContractorId = StringValidators.notEmpty
            .and(value -> value.startsWith("cont_"));

        // Complete Job validator
        public static final Validators.Validator<Job> validJob = job -> {
            if (job == null) return false;

            return SCDCommonValidators.validId.isValid(job.getId()) &&
                   SCDCommonValidators.validUid.isValid(job.getUid()) &&
                   SCDCommonValidators.validVersion.isValid(job.getVersion()) &&
                   validStatus.isValid(job.getStatus()) &&
                   validRate.isValid(job.getRate()) &&
                   validTitle.isValid(job.getTitle()) &&
                   validCompanyId.isValid(job.getCompanyId()) &&
                   validContractorId.isValid(job.getContractorId());
        };

        // Complete New Job validator
        public static final Validators.Validator<Job> validNewJob = job -> {
            if (job == null) return false;

            return validStatus.isValid(job.getStatus()) &&
                validRate.isValid(job.getRate()) &&
                validTitle.isValid(job.getTitle()) &&
                validCompanyId.isValid(job.getCompanyId()) &&
                validContractorId.isValid(job.getContractorId());
        };
    }

    /**
     * Timelog-specific validators
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TimelogValidators {
        // Duration validators
        public static final Validators.Validator<Long> validDuration = value ->
            value != null && value > 0 && value < 24 * 60 * 60 * 1000; // Less than 24 hours in milliseconds

        // Time range validators
        public static final Validators.Validator<Long> validTimestamp = value ->
            value != null && value > 0 && value <= Instant.now().toEpochMilli();

        public static Validators.Validator<Timelog> validTimeRange = timelog -> {
            if (timelog == null || timelog.getTimeStart() == null || timelog.getTimeEnd() == null) {
                return false;
            }

            return timelog.getTimeStart() < timelog.getTimeEnd() &&
                   timelog.getTimeEnd() - timelog.getTimeStart() == timelog.getDuration();
        };

        // Type validators
        public static final Validators.Validator<String> validType = StringValidators.notEmpty
            .and(value -> value.equals("captured") || value.equals("adjusted"));

        // Job UID validators
        public static final Validators.Validator<String> validJobUid = StringValidators.notEmpty
            .and(value -> value.startsWith("job_uid_"));

        // Complete Timelog validator
        public static final Validators.Validator<Timelog> validTimelog = timelog -> {
            if (timelog == null) return false;

            return SCDCommonValidators.validId.isValid(timelog.getId()) &&
                   SCDCommonValidators.validUid.isValid(timelog.getUid()) &&
                   SCDCommonValidators.validVersion.isValid(timelog.getVersion()) &&
                   validDuration.isValid(timelog.getDuration()) &&
                   validTimestamp.isValid(timelog.getTimeStart()) &&
                   validTimestamp.isValid(timelog.getTimeEnd()) &&
                   validTimeRange.isValid(timelog) &&
                   validType.isValid(timelog.getType()) &&
                   validJobUid.isValid(timelog.getJobUid());
        };

        // Complete New Timelog validator
        public static final Validators.Validator<Timelog> validNewTimelog = timelog -> {
            if (timelog == null) return false;

            return validDuration.isValid(timelog.getDuration()) &&
                validTimestamp.isValid(timelog.getTimeStart()) &&
                validTimestamp.isValid(timelog.getTimeEnd()) &&
                validTimeRange.isValid(timelog) &&
                validType.isValid(timelog.getType()) &&
                validJobUid.isValid(timelog.getJobUid());
        };
    }

    /**
     * PaymentLineItem-specific validators
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PaymentLineItemValidators {
        // Amount validators
        public static final Validators.Validator<BigDecimal> validAmount = BigDecimalValidators.notNull
            .and(BigDecimalValidators.isPositive)
            .and(BigDecimalValidators.hasMaxScale(2));

        // Status validators
        public static final Validators.Validator<String> validStatus = StringValidators.notEmpty
            .and(value -> value.equals("paid") || value.equals("not-paid") || value.equals("failed"));

        // Reference UID validators
        public static final Validators.Validator<String> validJobUid = StringValidators.notEmpty
            .and(value -> value.startsWith("job_uid_"));

        public static final Validators.Validator<String> validTimelogUid = StringValidators.notEmpty
            .and(value -> value.startsWith("tl_uid_"));

        // Complete PaymentLineItem validator
        public static final Validators.Validator<PaymentLineItem> validPaymentLineItem = item -> {
            if (item == null) return false;

            return SCDCommonValidators.validId.isValid(item.getId()) &&
                   SCDCommonValidators.validUid.isValid(item.getUid()) &&
                   SCDCommonValidators.validVersion.isValid(item.getVersion()) &&
                   validJobUid.isValid(item.getJobUid()) &&
                   validTimelogUid.isValid(item.getTimelogUid()) &&
                   validAmount.isValid(item.getAmount()) &&
                   validStatus.isValid(item.getStatus());
        };

        // Complete PaymentLineItem validator
        public static final Validators.Validator<PaymentLineItem> validNewPaymentLineItem = item -> {
            if (item == null) return false;

            return validJobUid.isValid(item.getJobUid()) &&
                validTimelogUid.isValid(item.getTimelogUid()) &&
                validAmount.isValid(item.getAmount()) &&
                validStatus.isValid(item.getStatus());
        };
    }

    /**
     * Business rule validators that span multiple entities
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BusinessRuleValidators {
        // Validate that a payment line item refers to the latest version of a job
        public static final Validators.Validator<PaymentLineItem> refersToLatestJobVersion =
            (paymentLineItem) -> {
                if (paymentLineItem == null || paymentLineItem.getJob() == null) {
                    return false;
                }

                // This would require a repository call to check if this is the latest version
                // For demonstration, we're assuming the job field is populated with the latest version
                return true;
            };

        // Validate that the payment amount matches the job rate and timelog duration
        public static final Validators.Validator<PaymentLineItem> amountMatchesRateAndDuration =
            (paymentLineItem) -> {
                if (paymentLineItem == null ||
                    paymentLineItem.getJob() == null ||
                    paymentLineItem.getTimelog() == null) {
                    return false;
                }

                BigDecimal hourlyRate = paymentLineItem.getJob().getRate();
                long durationMs = paymentLineItem.getTimelog().getDuration();

                // Convert duration from milliseconds to hours and calculate expected amount
                BigDecimal durationHours = BigDecimal.valueOf(durationMs)
                    .divide(BigDecimal.valueOf(3600000), 6, BigDecimal.ROUND_HALF_UP); // ms to hours

                BigDecimal expectedAmount = hourlyRate.multiply(durationHours)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

                return paymentLineItem.getAmount().compareTo(expectedAmount) == 0;
            };
    }
}
