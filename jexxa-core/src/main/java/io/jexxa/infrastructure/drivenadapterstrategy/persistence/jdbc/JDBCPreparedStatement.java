package io.jexxa.infrastructure.drivenadapterstrategy.persistence.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

class JDBCPreparedStatement
{
    private final Supplier<JDBCConnection> jdbcConnection;
    private final String sqlStatement;
    private final List<Object> arguments;

    JDBCPreparedStatement(Supplier<JDBCConnection> jdbcConnection, String sqlStatement, List<Object> arguments)
    {
        this.jdbcConnection = Objects.requireNonNull(jdbcConnection);
        this.sqlStatement = Objects.requireNonNull(sqlStatement);
        this.arguments = Objects.requireNonNull(arguments);
    }

    /**
     * This method creates a new PreparedStatement including sql statement and all arguments.
     *
     * Important note: The caller of this method is responsible to close the PreparedStatement, e.g. by calling it
     * in a try-with-resources statement.
     *
     * @return PreparedStatement that can be directly executed
     */
    protected PreparedStatement createPreparedStatement()
    {
        try
        {
            var preparedStatement = jdbcConnection.get().prepareStatement(sqlStatement);

            for (var i = 0; i < arguments.size(); ++i)
            {
                preparedStatement.setObject(i+1, arguments.get(i));
            }

            return preparedStatement;
        } catch (SQLException e)
        {
            throw new IllegalArgumentException("Invalid Query " + sqlStatement + " " + e.getMessage(), e);
        }
    }

    /**
     * This method creates a new PreparedStatement including sql statement and all arguments.
     *
     * Important note: The caller of this method is responsible to close the PreparedStatement, e.g. by calling it
     * in a try-with-resources statement.
     *
     * @deprecated This method is deprecated because it easy to misuse. Please use createPreparedStatement instead.
     *
     * @return PreparedStatement that can be directly executed
     */
    @Deprecated(forRemoval = true)
    public PreparedStatement getStatement()
    {
        return createPreparedStatement();
    }

    protected String getSQLStatement()
    {
        return sqlStatement;
    }

}
