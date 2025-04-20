package com.mercor.assignment.scd.domain.paymentlineitem.repository.impl;

import com.mercor.assignment.scd.domain.core.enums.EntityType;
import com.mercor.assignment.scd.domain.core.repository.impl.AbstractSCDRepositoryImpl;
import com.mercor.assignment.scd.domain.core.util.UidGenerator;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation of SCDRepositoryBase for Timelog entity
 * Extends AbstractSCDRepositoryImpl to inherit common SCD functionality
 */
@Repository
public class PaymentLineItemRepositoryImpl extends AbstractSCDRepositoryImpl<PaymentLineItem> {

    /**
     * Constructor with required dependencies
     *
     * @param uidGenerator the UID generator utility
     */
    @Autowired
    public PaymentLineItemRepositoryImpl(UidGenerator uidGenerator) {
        super(uidGenerator, PaymentLineItem.class, EntityType.PAYMENT_LINE_ITEMS);
    }

    @Override
    protected PaymentLineItem createEmptyEntity() {
        return new PaymentLineItem();
    }

    @Override
    protected void updateEntityFields(PaymentLineItem entity, Map<String, Object> fieldsToUpdate) {
        fieldsToUpdate.forEach((field, value) -> {
            switch (field) {
                case "job_uid":
                    entity.setJobUid((String) value);
                    break;
                case "timelog_uid":
                    entity.setTimelogUid((String) value);
                    break;
                case "amount":
                    if (value instanceof BigDecimal bigDecimal) {
                        entity.setAmount(bigDecimal);
                    } else if (value instanceof Number number) {
                        entity.setAmount(new BigDecimal(number.toString()));
                    } else if (value instanceof String string) {
                        entity.setAmount(new BigDecimal(string));
                    }
                    break;
                case "status":
                    entity.setStatus((String) value);
                    break;
            }
        });
    }
}