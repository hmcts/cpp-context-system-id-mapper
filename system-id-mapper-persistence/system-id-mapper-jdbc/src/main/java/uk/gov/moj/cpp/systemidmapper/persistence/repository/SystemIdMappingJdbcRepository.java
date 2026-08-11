package uk.gov.moj.cpp.systemidmapper.persistence.repository;


import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryException;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapper;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.api.SystemIdMappingRepository;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingConflictException;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingNotFoundException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import javax.sql.DataSource;

/**
 * JDBC based implementation of the {@link SystemIdMappingRepository}.
 */
public class SystemIdMappingJdbcRepository implements SystemIdMappingRepository {

    private static final String ON_CONFLICT = " ON CONFLICT DO NOTHING";
    private static final String INSERT = "INSERT INTO mapping (mapping_id, source_id, source_type, target_id, target_type, created_at) VALUES(?, ?, ?, ?, ?, ?)" + ON_CONFLICT;
    private static final String SELECT_BY_ID = "SELECT * FROM mapping WHERE mapping_id=?";
    private static final String SELECT_BY_SOURCE_ID_AND_TYPE = "SELECT * FROM mapping WHERE source_id=? AND source_type=? AND target_type=?";
    private static final String SELECT_BY_TARGET_ID_AND_TYPE = "SELECT * FROM mapping WHERE target_id=? AND target_type=?";
    private static final String SELECT_BY_SOURCE_ID_AND_TARGET_TYPE = "SELECT * FROM mapping WHERE source_id=? AND target_type IN (;)";
    private static final String UPDATE_MAPPING = "UPDATE mapping SET source_id = ? where mapping_id = ?";

    @Inject
    private UtcClock clock;

    @Inject
    private PreparedStatementWrapperFactory preparedStatementWrapperFactory;

    @Inject
    private SystemIdMapperDataSourceProvider systemIdMapperDataSourceProvider;

    @Override
    public Optional<SystemIdMapping> getSystemIdMapping(final UUID mappingId) {

        final DataSource dataSource = systemIdMapperDataSourceProvider.getDataSource();
        try (final PreparedStatementWrapper ps = preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                SELECT_BY_ID)) {

            ps.setObject(1, mappingId);

            return optionalEntityFrom(ps.executeQuery());
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Exception while returning mapping, mappingId: %s", mappingId.toString()), e);
        }
    }

    @Override
    public Optional<SystemIdMapping> findSystemIdMapping(final String sourceId, final String sourceType, final String targetType) {

        final DataSource dataSource = systemIdMapperDataSourceProvider.getDataSource();
        try (final PreparedStatementWrapper ps = preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                SELECT_BY_SOURCE_ID_AND_TYPE)) {

            ps.setString(1, sourceId);
            ps.setString(2, sourceType);
            ps.setString(3, targetType);

            return optionalEntityFrom(ps.executeQuery());
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Exception while returning mapping, sourceId: %s, sourceType: %s", sourceId, sourceType), e);
        }
    }

    @Override
    public Optional<SystemIdMapping> findBySourceIdAndTargetType(final String sourceId, final String... targetTypes) {

        final String findBySourceIdAndTargetTypeQuery = buildDynamicQuery(targetTypes);
        final DataSource dataSource = systemIdMapperDataSourceProvider.getDataSource();
        try (final PreparedStatementWrapper ps = preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource, findBySourceIdAndTargetTypeQuery)) {

            ps.setString(1, sourceId);
            int index = 2;

            for (final String targetType : targetTypes) {
                ps.setString(index++, targetType);
            }

            return optionalEntityFrom(ps.executeQuery());
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Exception while returning mapping, sourceId: %s, targetTypes: %s", sourceId, Arrays.toString(targetTypes)), e);
        }
    }

    String buildDynamicQuery(final String[] targetTypes) {
        final StringBuilder builder = new StringBuilder();
        Arrays.stream(targetTypes).map(targetType -> "?,").forEach(builder::append);
        return SELECT_BY_SOURCE_ID_AND_TARGET_TYPE.replace(";", builder.deleteCharAt(builder.length() - 1).toString());
    }

    @Override
    public Optional<SystemIdMapping> findSystemIdMapping(final UUID targetId, final String targetType) {

        final DataSource dataSource = systemIdMapperDataSourceProvider.getDataSource();
        try (final PreparedStatementWrapper ps = preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                SELECT_BY_TARGET_ID_AND_TYPE)) {
            ps.setObject(1, targetId);
            ps.setString(2, targetType);

            return optionalEntityFrom(ps.executeQuery());
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Exception while returning mapping, targetId: %s, targetType: %s", targetId, targetType), e);
        }
    }

    @Override
    public UUID insertSystemIdMapping(final String sourceId, final String sourceType, final UUID targetId, final String targetType) throws MappingConflictException {

        final DataSource dataSource = systemIdMapperDataSourceProvider.getDataSource();
        try (final PreparedStatementWrapper ps = preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                INSERT)) {
            final UUID mappingId = randomUUID();
            final ZonedDateTime createdAt = clock.now();

            ps.setObject(1, mappingId);
            ps.setString(2, sourceId);
            ps.setString(3, sourceType);
            ps.setObject(4, targetId);
            ps.setString(5, targetType);
            ps.setTimestamp(6, toSqlTimestamp(createdAt));

            final int updateCount = ps.executeUpdate();

            if (updateCount == 0) {
                throw new MappingConflictException(format("Mapping already exists for mapping '%s:%s' of target type '%s'", sourceType, sourceId, targetType));
            }
            return mappingId;
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Exception while storing mapping '%s:%s' of target type '%s' in the database", sourceType, sourceId, targetType), e);
        }
    }

    @Override
    public void remapSystemIdMapping(final String newSourceId, final UUID mappingId) {

        final DataSource dataSource = systemIdMapperDataSourceProvider.getDataSource();
        try (final PreparedStatementWrapper ps = preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                UPDATE_MAPPING)) {
            ps.setString(1, newSourceId);
            ps.setObject(2, mappingId);

            final int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) {
                throw new MappingNotFoundException(format("Unable to update mapping with id '%s': No mapping found", mappingId));
            }
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Failed to update mapping with mapping_id '%s'", mappingId), e);
        }
    }

    protected Optional<SystemIdMapping> optionalEntityFrom(final ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return Optional.of(new SystemIdMapping(
                    (UUID) resultSet.getObject("mapping_id"),
                    resultSet.getString("source_id"), resultSet.getString("source_type"),
                    (UUID) resultSet.getObject("target_id"),
                    resultSet.getString("target_type"),
                    fromSqlTimestamp(resultSet.getTimestamp("created_at"))));

        }
        return Optional.empty();
    }
}
