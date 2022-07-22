package io.jexxa.jexxatest.architecture.validapplication.domain.aggregate;

import io.jexxa.addend.applicationcore.Aggregate;
import io.jexxa.addend.applicationcore.AggregateID;
import io.jexxa.jexxatest.architecture.validapplication.domain.valueobject.ValidValueObject;

@Aggregate
public class ValidAggregate {
    private final ValidValueObject validValueObjectA;

    public ValidAggregate(ValidValueObject validValueObject)
    {
        this.validValueObjectA = validValueObject;
    }

    @AggregateID
    public ValidValueObject getValidValueObjectA()
    {
        return validValueObjectA;
    }

}
