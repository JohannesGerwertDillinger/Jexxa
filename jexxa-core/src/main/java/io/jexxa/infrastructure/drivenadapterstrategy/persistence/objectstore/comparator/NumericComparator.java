package io.jexxa.infrastructure.drivenadapterstrategy.persistence.objectstore.comparator;

import java.util.function.Function;

/**
 * A comparator provides a strategy to compare a specific element of an aggregate.
 *
 * Note: The specific value must be convertible to a number
 *
 * @param <T> Defines the type of the aggregate
 * @param <S> Defines the type of the value inside the aggregate
 */
public class NumericComparator<T, S>  extends Comparator<T, S, Number>
{
    /**
     * Creates an Comparator object
     *
     * @param valueAccessor defines a function to access a specific value of the aggregate
     * @param valueConverter defines a function that converts a searched value into a Number for comparison
     */
    public NumericComparator(Function<T, S> valueAccessor,
                                     Function<S, ? extends Number> valueConverter)
    {
        super(valueAccessor, valueConverter);
    }

    @Override
    public Class<Number> getValueType()
    {
        return Number.class;
    }

}

