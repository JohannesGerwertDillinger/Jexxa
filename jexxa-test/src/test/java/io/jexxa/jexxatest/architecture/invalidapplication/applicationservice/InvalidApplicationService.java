package io.jexxa.jexxatest.architecture.invalidapplication.applicationservice;

import io.jexxa.jexxatest.architecture.invalidapplication.domain.aggregate.InvalidAggregate;
import io.jexxa.jexxatest.architecture.invalidapplication.domain.valueobject.InvalidValueObject;
import io.jexxa.jexxatest.architecture.invalidapplication.domainservice.InvalidRepository;
@SuppressWarnings("unused")

public class InvalidApplicationService
{
    private final InvalidRepository invalidRepository;

    public InvalidApplicationService(InvalidRepository invalidRepository)
    {
        this.invalidRepository = invalidRepository;
    }

    public InvalidAggregate get(InvalidValueObject invalidValueObject)
    {
        return invalidRepository.get(invalidValueObject);
    }

}
