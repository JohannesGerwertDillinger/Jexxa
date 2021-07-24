package io.jexxa.infrastructure.drivenadapterstrategy.persistence.objectstore.jdbc;

import static io.jexxa.utils.json.JSONManager.getJSONConverter;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.jexxa.infrastructure.drivenadapterstrategy.persistence.jdbc.JDBCConnection;
import io.jexxa.infrastructure.drivenadapterstrategy.persistence.jdbc.JDBCQuery;
import io.jexxa.infrastructure.drivenadapterstrategy.persistence.jdbc.builder.SQLOrder;
import io.jexxa.infrastructure.drivenadapterstrategy.persistence.objectstore.IStringQuery;
import io.jexxa.infrastructure.drivenadapterstrategy.persistence.objectstore.comparator.MetadataComparator;
import io.jexxa.infrastructure.drivenadapterstrategy.persistence.objectstore.comparator.StringComparator;
import io.jexxa.utils.json.JSONConverter;

public class JDBCStringQuery <T, S, M extends Enum<M> & MetadataComparator> implements IStringQuery<T, S>
{
    private final Supplier<JDBCConnection> jdbcConnection;
    private final StringComparator<T, S> stringComparator;

    private final Class<T> aggregateClazz;
    private final JSONConverter jsonConverter = getJSONConverter();
    private final M nameOfRow;
    private final M schemaValue;
    private final Class<M> comparatorSchema;

    @SuppressWarnings("unused") //Type required for java type inference
    private final Class<S> queryType;

    public JDBCStringQuery(
            Supplier<JDBCConnection> jdbcConnection,
            StringComparator<T, S> stringComparator,
            M nameOfRow,
            Class<T> aggregateClazz,
            Class<M> comparatorSchema,
            Class<S> queryType
    )
    {
        this.jdbcConnection = Objects.requireNonNull( jdbcConnection );
        this.aggregateClazz = Objects.requireNonNull(aggregateClazz);
        this.nameOfRow = Objects.requireNonNull(nameOfRow);
        this.stringComparator = Objects.requireNonNull(stringComparator);
        this.comparatorSchema = Objects.requireNonNull(comparatorSchema);
        this.queryType = Objects.requireNonNull(queryType);

        var comparatorFunctions = EnumSet.allOf(comparatorSchema);
        var iterator = comparatorFunctions.iterator();
        iterator.next();
        schemaValue = iterator.next();
    }
    @Override
    public List<T> beginsWith(S value)
    {
        var sqlStartValue = stringComparator.convertValue(value) + "%";

        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .where(nameOfRow)
                .like(sqlStartValue)
                .orderBy(nameOfRow, SQLOrder.ASC)
                .create();

        return searchElements(jdbcQuery);
    }

    @Override
    public List<T> endsWith(S value)
    {
        var sqlEndValue = "%" + stringComparator.convertValue(value);

        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .where(nameOfRow)
                .like(sqlEndValue)
                .orderBy(nameOfRow, SQLOrder.ASC)
                .create();

        return searchElements(jdbcQuery);
    }

    @Override
    public List<T> includes(S value)
    {
        var sqlIncludeValue = "%" + stringComparator.convertValue(value) + "%";

        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .where(nameOfRow)
                .like(sqlIncludeValue)
                .orderBy(nameOfRow, SQLOrder.ASC)
                .create();

        return searchElements(jdbcQuery);
    }

    @Override
    public List<T> isEqualTo(S value)
    {
        var sqlEqualValue = stringComparator.convertValue(value) ;

        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .where(nameOfRow)
                .like(sqlEqualValue)
                .orderBy(nameOfRow, SQLOrder.ASC)
                .create();

        return searchElements(jdbcQuery);    }

    @Override
    public List<T> notIncludes(S value)
    {
        var sqlIncludeValue = "%" + stringComparator.convertValue(value) + "%";

        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .where(nameOfRow)
                .notLike(sqlIncludeValue)
                .orderBy(nameOfRow, SQLOrder.ASC)
                .create();

        return searchElements(jdbcQuery);
    }

    @Override
    public List<T> getAscending(int amount)
    {
        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .orderBy(nameOfRow, SQLOrder.ASC)
                .limit(amount)
                .create();

        return searchElements( jdbcQuery );
    }

    @Override
    public List<T> getAscending()
    {
        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .orderBy(nameOfRow, SQLOrder.ASC)
                .create();

        return searchElements( jdbcQuery );
    }

    @Override
    public List<T> getDescending(int amount)
    {
        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .orderBy(nameOfRow, SQLOrder.DESC)
                .limit(amount)
                .create();

        return searchElements( jdbcQuery );
    }

    @Override
    public List<T> getDescending()
    {
        var jdbcQuery = jdbcConnection.get()
                .createQuery(comparatorSchema)
                .select( schemaValue )
                .from(aggregateClazz)
                .orderBy(nameOfRow, SQLOrder.DESC)
                .create();

        return searchElements( jdbcQuery );
    }

    protected List<T> searchElements(JDBCQuery query)
    {
        return query.asString()
                .flatMap(Optional::stream)
                .map( element -> jsonConverter.fromJson(element, aggregateClazz))
                .collect(Collectors.toList());
    }

}
