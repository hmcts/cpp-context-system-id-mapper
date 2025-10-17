package uk.gov.moj.cpp.systemidmapper.integration;

import static java.time.ZonedDateTime.now;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

public class DatabaseSeeder {

    private static final String USERNAME = "system";
    private static final String PASSWORD = "system";
    private static final String DATABASE = "systemidmapper";

    private static final String INSERT_SQL = "INSERT INTO mapping (" +
            "mapping_id, source_id, source_type, target_id, target_type, created_at) \n" +
            "VALUES(?, ?, ?, ?, ?, ?)";

    private static final String DELETE_SQL = "DELETE FROM mapping";

    private final ConnectionProvider connectionProvider = new ConnectionProvider();

    public void cleanTables() throws SQLException {
        try (final Connection connection = connectionProvider.getNewConnection(USERNAME, PASSWORD, DATABASE);
             final PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL)) {
            preparedStatement.executeUpdate();
        }
    }

    public void insertMapping(
            final UUID mappingId,
            final String sourceId,
            final String sourceType,
            final UUID targetId,
            final String targetType
    ) throws SQLException {


        try (final Connection connection = connectionProvider.getNewConnection(USERNAME, PASSWORD, DATABASE);
             final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {

            preparedStatement.setObject(1, mappingId);
            preparedStatement.setString(2, sourceId);
            preparedStatement.setString(3, sourceType);
            preparedStatement.setObject(4, targetId);
            preparedStatement.setObject(5, targetType);
            preparedStatement.setTimestamp(6, asTimestamp(now()));

            preparedStatement.executeUpdate();
        }
    }

    private Timestamp asTimestamp(final ZonedDateTime dateTime) {
        return new Timestamp(dateTime.toInstant().getEpochSecond() * 1000L);
    }
}
