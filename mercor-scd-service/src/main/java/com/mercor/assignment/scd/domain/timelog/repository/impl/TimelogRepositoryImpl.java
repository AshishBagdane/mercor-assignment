package com.mercor.assignment.scd.domain.timelog.repository.impl;

import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.repository.impl.AbstractSCDRepositoryImpl;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation of SCDRepositoryBase for Timelog entity
 * Extends AbstractSCDRepositoryImpl to inherit common SCD functionality
 */
@Repository
public class TimelogRepositoryImpl extends AbstractSCDRepositoryImpl<Timelog> {

    /**
     * Constructor with required dependencies
     *
     * @param uidGenerator the UID generator utility
     */
    @Autowired
    public TimelogRepositoryImpl(UidGenerator uidGenerator) {
        super(uidGenerator, Timelog.class, EntityType.TIMELOG);
    }

    @Override
    protected Timelog createEmptyEntity() {
        return new Timelog();
    }

    @Override
    protected void updateEntityFields(Timelog entity, Map<String, Object> fieldsToUpdate) {
        fieldsToUpdate.forEach((field, value) -> {
            switch (field) {
                case "duration":
                    if (value instanceof Long) {
                        entity.setDuration((Long) value);
                    } else if (value instanceof Number) {
                        entity.setDuration(((Number) value).longValue());
                    } else if (value instanceof String) {
                        entity.setDuration(Long.parseLong((String) value));
                    }
                    break;
                case "time_start":
                    if (value instanceof Long) {
                        entity.setTimeStart((Long) value);
                    } else if (value instanceof Number) {
                        entity.setTimeStart(((Number) value).longValue());
                    } else if (value instanceof String) {
                        entity.setTimeStart(Long.parseLong((String) value));
                    }
                    break;
                case "time_end":
                    if (value instanceof Long) {
                        entity.setTimeEnd((Long) value);
                    } else if (value instanceof Number) {
                        entity.setTimeEnd(((Number) value).longValue());
                    } else if (value instanceof String) {
                        entity.setTimeEnd(Long.parseLong((String) value));
                    }
                    break;
                case "type":
                    entity.setType((String) value);
                    break;
                case "job_uid":
                    entity.setJobUid((String) value);
                    break;
            }
        });
    }
}