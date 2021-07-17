package io.jexxa.infrastructure.drivenadapterstrategy.persistence.comparator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Function;


@SuppressWarnings({"unused","java:S1452"})
public class Comparators
{
    /**
     *  Reserved Comparator to identify the Key of an aggregate in the {@link MetadataComparator}
     *
     * @param <T> type of the aggregate
     * @param <V> type of the key
     * @return Comparator to identify the key
     */
    public static  <T, V> NumericComparator<T, V> keyComparator()
    {
        return null;
    }

    /**
     *  Reserved Comparator to identify the aggregate itself in the {@link MetadataComparator}
     *
     * @param <T> type of the aggregate
     * @param <V> type of the key
     * @return Comparator to identify the key
     */
    public static  <T, V> NumericComparator<T, V> valueComparator()
    {
        return null;
    }

    /**
     * Factory method to create comparator wich compares value of an aggregate using defined converter function
     *
     * @param accessorFunction defines the method to get the value to be compared
     * @param converterFunction defines the converter function converting the value into a number
     * @param <T> type of the aggregate
     * @param <V> type of the value
     * @return Comparator wich compares defined value of an aggregate
     */
    public static <T, V> NumericComparator<T, V> converterComparator(Function<T, V> accessorFunction, Function<V, ? extends Number> converterFunction)
    {
        return new NumericComparator<>(accessorFunction, converterFunction);
    }

    /**
     * Factory method to create comparator wich compares value of type {@link Instant} of an aggregate
     *
     * @param accessorFunction returns the Instant
     * @param <T> type of the aggregate
     * @return Comparator wich compares an {@link Instant} of an aggregate
     */
    public static <T> InstantNumericComparator<T> instantComparator(Function<T, Instant> accessorFunction )
    {
        return new InstantNumericComparator<>(accessorFunction);
    }

    /**
     * Factory method to create comparator wich compares value of type {@link Instant} of an aggregate
     *
     * @param accessorFunction returns the Instant
     * @param <T> type of the aggregate
     * @return Comparator wich compares an {@link Instant} of an aggregate
     */
    public static <T> NumericComparator<T, Boolean> booleanComparator(Function<T, Boolean> accessorFunction )
    {
        return new NumericComparator<>(accessorFunction, value -> value ? 1 : 0 );
    }

    /**
     * Factory method to create comparator wich compares value of type {@link Number} of an aggregate
     *
     * @param accessorFunction returns the Instant
     * @param <T> type of the aggregate
     * @return Comparator wich compares an {@link Number} of an aggregate
     */
    public static <T, V extends Number> NumericComparator<T, V> numberComparator(Function<T,V> accessorFunction )
    {
        return new NumberNumericComparator<>(accessorFunction);
    }

    private static class InstantNumericComparator<T> extends NumericComparator<T, Instant>
    {
        public static final int NANO = 1000000000;
        public InstantNumericComparator(Function<T, Instant> accessorFunction)
        {
            super(accessorFunction
                    , instant -> BigDecimal.valueOf(instant.getEpochSecond() * NANO).add( BigDecimal.valueOf(instant.getNano()))
                    );
        }

    }

    private static class NumberNumericComparator<T, V extends Number> extends NumericComparator<T, V>
    {
        public NumberNumericComparator(Function<T, V> accessorFunction)
        {
            super( accessorFunction, element -> element);
        }
    }



    private Comparators()
    {
        //private constructor
    }


}
