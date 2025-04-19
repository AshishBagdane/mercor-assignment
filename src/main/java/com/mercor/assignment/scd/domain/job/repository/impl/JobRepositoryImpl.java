package com.mercor.assignment.scd.domain.job.repository.impl;

import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.repository.impl.AbstractSCDRepositoryImpl;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.job.model.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Implementation of SCDRepositoryBase for Job entity
 * Extends AbstractSCDRepositoryImpl to inherit common SCD functionality
 */
@Repository
public class JobRepositoryImpl extends AbstractSCDRepositoryImpl<Job> {

    /**
     * Constructor with required dependencies
     *
     * @param uidGenerator the UID generator utility
     */
    @Autowired
    public JobRepositoryImpl(UidGenerator uidGenerator) {
        super(uidGenerator, Job.class, EntityType.JOBS);
    }

    @Override
    protected Job createEmptyEntity() {
        return new Job();
    }

    @Override
    protected void updateEntityFields(Job entity, Map<String, Object> fieldsToUpdate) {
        fieldsToUpdate.forEach((field, value) -> {
            switch (field) {
                case "status":
                    entity.setStatus((String) value);
                    break;
                case "rate":
                    if (value instanceof BigDecimal) {
                        entity.setRate((BigDecimal) value);
                    } else if (value instanceof Number) {
                        entity.setRate(new BigDecimal(value.toString()));
                    } else if (value instanceof String) {
                        entity.setRate(new BigDecimal((String) value));
                    }
                    break;
                case "title":
                    entity.setTitle((String) value);
                    break;
                case "companyId":
                    entity.setCompanyId((String) value);
                    break;
                case "contractorId":
                    entity.setContractorId((String) value);
                    break;
            }
        });
    }

    @Override
    protected void copyEntityData(Job source, Job target) {
        // Copy all business fields from source to target
        target.setStatus(source.getStatus());
        target.setRate(source.getRate());
        target.setTitle(source.getTitle());
        target.setCompanyId(source.getCompanyId());
        target.setContractorId(source.getContractorId());

        // Add any other business fields that need to be copied
        // DO NOT copy id, version, uid, createdAt, updatedAt fields here
    }
}