package uk.gov.moj.cpp.systemidmapper.persistence.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryException;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapper;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingConflictException;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingNotFoundException;

import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemIdMappingJdbcRepositoryTest  {

    @Mock
    private UtcClock clock;

    @Mock
    private PreparedStatementWrapperFactory preparedStatementWrapperFactory;

    @Mock
    private SystemIdMapperDataSourceProvider systemIdMapperDataSourceProvider;

    @InjectMocks
    private SystemIdMappingJdbcRepository  systemIdMappingJdbcRepository;

    @Test
    public void shouldInsertNewMappingAndReturnCreatedMappingId() throws MappingConflictException {
        final String expectedQuery ="SELECT * FROM mapping WHERE source_id=? AND target_type IN (?,?,?,?)";
        final String[] targetTypes = {"CASE-ID","CASE_ID","CASE_FILE_ID","caseId"};

        final String generatedQuery = systemIdMappingJdbcRepository.buildDynamicQuery(targetTypes);
        assertThat(generatedQuery, is(equalTo(expectedQuery)));
    }

    @Test
    public void shouldUpdateTheSourceId() throws Exception {

        final String newSourceId = "newSourceId";
        final UUID mappingId = randomUUID();
        final int rowsUpdated = 29;

        final DataSource dataSource = mock(DataSource.class);
        final PreparedStatementWrapper preparedStatementWrapper = mock(PreparedStatementWrapper.class);

        when(systemIdMapperDataSourceProvider.getDataSource()).thenReturn(dataSource);
        when(preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                "UPDATE mapping SET source_id = ? where mapping_id = ?")).thenReturn(preparedStatementWrapper);
        when(preparedStatementWrapper.executeUpdate()).thenReturn(rowsUpdated);

        systemIdMappingJdbcRepository.remapSystemIdMapping(newSourceId, mappingId);

        final InOrder inOrder = inOrder(preparedStatementWrapper);

        inOrder.verify(preparedStatementWrapper).setString(1, newSourceId);
        inOrder.verify(preparedStatementWrapper).setObject(2, mappingId);
        inOrder.verify(preparedStatementWrapper).executeUpdate();
    }

    @Test
    public void shouldFailToUpdateTheSourceIdIfNoMappingFound() throws Exception {

        final String newSourceId = "newSourceId";
        final UUID mappingId = fromString("80a90027-6f68-478e-b587-4e10d648dbea");
        final int rowsUpdated = 0;

        final DataSource dataSource = mock(DataSource.class);
        final PreparedStatementWrapper preparedStatementWrapper = mock(PreparedStatementWrapper.class);

        when(systemIdMapperDataSourceProvider.getDataSource()).thenReturn(dataSource);
        when(preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                "UPDATE mapping SET source_id = ? where mapping_id = ?")).thenReturn(preparedStatementWrapper);
        when(preparedStatementWrapper.executeUpdate()).thenReturn(rowsUpdated);

        try {
            systemIdMappingJdbcRepository.remapSystemIdMapping(newSourceId, mappingId);
            fail();
        } catch (final MappingNotFoundException expected) {
            assertThat(expected.getMessage(), is("Unable to update mapping with id '80a90027-6f68-478e-b587-4e10d648dbea': No mapping found"));
        }
    }

    @Test
    public void shouldFailIfUpdatingTheMappingFails() throws Exception {

        final SQLException sqlException = new SQLException("Ooops");

        final String newSourceId = "newSourceId";
        final UUID mappingId = fromString("e2dc129c-993c-459b-ae36-9c1ad6af9431");

        final DataSource dataSource = mock(DataSource.class);
        final PreparedStatementWrapper preparedStatementWrapper = mock(PreparedStatementWrapper.class);

        when(systemIdMapperDataSourceProvider.getDataSource()).thenReturn(dataSource);
        when(preparedStatementWrapperFactory.preparedStatementWrapperOf(
                dataSource,
                "UPDATE mapping SET source_id = ? where mapping_id = ?")).thenReturn(preparedStatementWrapper);

        when(preparedStatementWrapper.executeUpdate()).thenThrow(sqlException);

        try {
            systemIdMappingJdbcRepository.remapSystemIdMapping(newSourceId, mappingId);
            fail();
        } catch (final JdbcRepositoryException expected) {
            assertThat(expected.getCause(), is(sqlException));
            assertThat(expected.getMessage(), is("Failed to update mapping with mapping_id 'e2dc129c-993c-459b-ae36-9c1ad6af9431'"));
        }
    }
}