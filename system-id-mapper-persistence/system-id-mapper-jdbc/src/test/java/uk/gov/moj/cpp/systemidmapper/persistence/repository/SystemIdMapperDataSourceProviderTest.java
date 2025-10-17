package uk.gov.moj.cpp.systemidmapper.persistence.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.jdbc.persistence.JdbcDataSourceProvider;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class SystemIdMapperDataSourceProviderTest {

    @Mock
    private JdbcDataSourceProvider jdbcDataSourceProvider;

    @InjectMocks
    private SystemIdMapperDataSourceProvider systemIdMapperDataSourceProvider;

    @Test
    public void shouldGetTheSystemIdMapperDataSourceUsingTheCorrectJndiName() throws Exception {

        final DataSource dataSource = mock(DataSource.class);

        when(jdbcDataSourceProvider.getDataSource("java:/DS.systemidmapper")).thenReturn(dataSource);

        assertThat(systemIdMapperDataSourceProvider.getDataSource(), is(dataSource));
    }
}