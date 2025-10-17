package uk.gov.moj.cpp.systemidmapper.healthchecks;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.systemidmapper.healthchecks.IdMapperViewstoreDatabaseHealthcheck.ID_MAPPER_VIEWSTORE_DATABASE_HEALTHCHECK_NAME;

import uk.gov.justice.services.healthcheck.api.HealthcheckResult;
import uk.gov.justice.services.healthcheck.utils.database.TableChecker;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.SystemIdMapperDataSourceProvider;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class IdMapperViewstoreDatabaseHealthcheckTest {

    @Mock
    private SystemIdMapperDataSourceProvider systemIdMapperDataSourceProvider;

    @Mock
    private TableChecker tableChecker;

    @Mock
    private Logger logger;

    @InjectMocks
    private IdMapperViewstoreDatabaseHealthcheck idMapperViewstoreDatabaseHealthcheck;

    @Test
    public void shouldReturnCorrectHealthcheckName() throws Exception {

        assertThat(idMapperViewstoreDatabaseHealthcheck.getHealthcheckName(), is(ID_MAPPER_VIEWSTORE_DATABASE_HEALTHCHECK_NAME));
    }

    @Test
    public void shouldReturnCorrectHealthcheckDescription() throws Exception {

        assertThat(idMapperViewstoreDatabaseHealthcheck.healthcheckDescription(), is("Checks connectivity to the system-id-mapper viewstore database and that all its tables are available"));
    }

    @Test
    public void shouldGetListOfExpectedTablesFromEventStoreAsHealthcheck() throws Exception {

        final DataSource systemDataSource = mock(DataSource.class);
        final HealthcheckResult healthcheckResult = mock(HealthcheckResult.class);

        when(systemIdMapperDataSourceProvider.getDataSource()).thenReturn(systemDataSource);
        when(tableChecker.checkTables(List.of("mapping"), systemDataSource)).thenReturn(healthcheckResult);

        assertThat(idMapperViewstoreDatabaseHealthcheck.runHealthcheck(), is(healthcheckResult));
    }

    @Test
    public void shouldReturnHealthcheckFailureIfAccessingTheEventStoreThrowsSqlException() throws Exception {

        final SQLException sqlException = new SQLException("Oops");
        final DataSource systemDataSource = mock(DataSource.class);

        when(systemIdMapperDataSourceProvider.getDataSource()).thenReturn(systemDataSource);
        when(tableChecker.checkTables(List.of("mapping"), systemDataSource)).thenThrow(sqlException);

        final HealthcheckResult healthcheckResult = idMapperViewstoreDatabaseHealthcheck.runHealthcheck();

        assertThat(healthcheckResult.isPassed(), is(false));
        assertThat(healthcheckResult.getErrorMessage().isPresent(), is(true));
        assertThat(healthcheckResult.getErrorMessage(), is(of("Exception thrown accessing system-id-mapper viewstore database. java.sql.SQLException: Oops")));

        verify(logger).error("Healthcheck for system-id-mapper viewstore database failed.", sqlException);
    }

}