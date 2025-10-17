package uk.gov.moj.cpp.systemidmapper.persistence.repository;

import uk.gov.justice.services.jdbc.persistence.JdbcDataSourceProvider;

import javax.inject.Inject;
import javax.sql.DataSource;

public class SystemIdMapperDataSourceProvider {

    private static final String DATASOURCE_JNDI_NAME = "java:/DS.systemidmapper";

    @Inject
    private JdbcDataSourceProvider jdbcDataSourceProvider;

    public DataSource getDataSource() {
        return jdbcDataSourceProvider.getDataSource(DATASOURCE_JNDI_NAME);
    }
}
