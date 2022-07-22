package io.jexxa.jexxatest.architecture.validapplication.infrastructure.drivingadapter;

import io.jexxa.addend.infrastructure.DrivingAdapter;
import io.jexxa.jexxatest.architecture.validapplication.applicationservice.InvalidApplicationService;

@DrivingAdapter
public class ValidDrivingAdapter {
    private final InvalidApplicationService invalidApplicationService;

    public ValidDrivingAdapter(InvalidApplicationService invalidApplicationService)
    {
        this.invalidApplicationService = invalidApplicationService;
    }
}
