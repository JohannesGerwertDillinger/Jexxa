package io.jexxa.infrastructure.drivenadapterstrategy.persistence.jdbc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import io.jexxa.infrastructure.drivenadapterstrategy.persistence.IRepository;
import io.jexxa.utils.JexxaLogger;
import org.slf4j.Logger;

public class JDBCKeyValueRepository<T, K> extends JDBCRepository implements IRepository<T, K>
{
    /**
     * @deprecated use constant {@link JDBCConnection#JDBC_URL} instead
     */
    @Deprecated(forRemoval = true)
    public static final String JDBC_URL = "io.jexxa.jdbc.url";

    /**
     * @deprecated use constant {@link JDBCConnection#JDBC_USERNAME} instead
     */
    @Deprecated(forRemoval = true)
    public static final String JDBC_USERNAME = "io.jexxa.jdbc.username";

    /**
     * @deprecated use constant {@link JDBCConnection#JDBC_PASSWORD} instead
     */
    @Deprecated(forRemoval = true)
    public static final String JDBC_PASSWORD = "io.jexxa.jdbc.password";

    /**
     * @deprecated use constant {@link JDBCConnection#JDBC_DRIVER} instead
     */
    @Deprecated(forRemoval = true)
    public static final String JDBC_DRIVER = "io.jexxa.jdbc.driver";

    /**
     * @deprecated use constant {@link JDBCConnection#JDBC_AUTOCREATE_TABLE} instead
     */
    @Deprecated(forRemoval = true)
    public static final String JDBC_AUTOCREATE_TABLE = "io.jexxa.jdbc.autocreate.table";

    /**
     * @deprecated use constant {@link JDBCConnection#JDBC_AUTOCREATE_DATABASE} instead
     */
    @Deprecated(forRemoval = true)
    public static final String JDBC_AUTOCREATE_DATABASE = "io.jexxa.jdbc.autocreate.database";


    private static final Logger LOGGER = JexxaLogger.getLogger(JDBCKeyValueRepository.class);

    private final Function<T,K> keyFunction;
    private final Class<T> aggregateClazz;
    private final Gson gson = new Gson();


    public JDBCKeyValueRepository(Class<T> aggregateClazz, Function<T,K> keyFunction, Properties properties)
    {
        super(properties);

        this.keyFunction = keyFunction;
        this.aggregateClazz = aggregateClazz;

        autocreateTable(properties);
    }


    @Override
    public void remove(K key)
    {
        Objects.requireNonNull(key);

        String command = String.format("delete from %s where key= '%s'"
                , getAggregateName()
                , gson.toJson(key));

        getConnection()
                .execute(command)
                .asUpdate();
    }

    @Override
    public void removeAll()
    {
        String command = String.format("delete from %s", getAggregateName());

        getConnection()
                .execute(command)
                .asIgnore();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void add(T aggregate)
    {
        Objects.requireNonNull(aggregate);

        String command = String.format("insert into %s values( '%s' , '%s' )"
                , getAggregateName()
                , gson.toJson(keyFunction.apply(aggregate))
                , gson.toJson(aggregate));

        getConnection()
                .execute(command)
                .asUpdate();
    }

    @SuppressWarnings({"DuplicatedCode", "unused"})
    @Override
    public void update(T aggregate)
    {
        Objects.requireNonNull(aggregate);

        String command = String.format("update %s set value = '%s' where key = '%s'"
                , getAggregateName()
                , gson.toJson(aggregate)
                , gson.toJson(keyFunction.apply(aggregate)));

        getConnection()
                .execute(command)
                .asUpdate();
    }

    @Override
    public Optional<T> get(K primaryKey)
    {
        Objects.requireNonNull(primaryKey);

        String sqlQuery = String.format( "select value from %s where key = '%s'"
                , getAggregateName()
                , gson.toJson(primaryKey));

        return getConnection()
                .query(sqlQuery)
                .asString()
                .flatMap(Optional::stream)
                .findFirst()
                .map( element -> gson.fromJson(element, aggregateClazz))
                .or(Optional::empty);
    }

    @Override
    public List<T> get()
    {
        return getConnection()
                .query("select value from "+ getAggregateName())
                .asString()
                .flatMap(Optional::stream)
                .map( element -> gson.fromJson(element, aggregateClazz))
                .collect(Collectors.toList());
    }


    private void autocreateTable(final Properties properties)
    {
        if (properties.containsKey(JDBCConnection.JDBC_AUTOCREATE_TABLE))
        {
            try{
                var command = String.format("CREATE TABLE IF NOT EXISTS %s ( key VARCHAR %s PRIMARY KEY, value text) "
                        , aggregateClazz.getSimpleName()
                        , getMaxVarChar(properties.getProperty(JDBCConnection.JDBC_URL)));

                getConnection()
                        .execute(command)
                        .asIgnore();
            }
            catch (RuntimeException e)
            {
                LOGGER.warn("Could not create table {} => Assume that table already exists", getAggregateName());
            }
        }
    }

    protected String getAggregateName()
    {
        return aggregateClazz.getSimpleName();
    }

    private static String getMaxVarChar(String jdbcDriver)
    {
        if ( jdbcDriver.toLowerCase().contains("oracle") )
        {
            return "(4000)";
        }

        if ( jdbcDriver.toLowerCase().contains("postgres") )
        {
            return ""; // Note in general Postgres does not have a real upper limit.
        }

        if ( jdbcDriver.toLowerCase().contains("h2") )
        {
            return "(" + Integer.MAX_VALUE + ")";
        }

        if ( jdbcDriver.toLowerCase().contains("mysql") )
        {
            return "(65535)";
        }

        return "(255)";
    }

}
