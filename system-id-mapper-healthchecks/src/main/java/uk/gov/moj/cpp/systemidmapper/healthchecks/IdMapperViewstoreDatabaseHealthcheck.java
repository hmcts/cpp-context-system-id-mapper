package uk.gov.moj.cpp.systemidmapper.healthchecks;

import static java.lang.String.format;
import static java.util.List.of;
import static uk.gov.justice.services.healthcheck.api.HealthcheckResult.failure;

import uk.gov.justice.services.healthcheck.api.Healthcheck;
import uk.gov.justice.services.healthcheck.api.HealthcheckResult;
import uk.gov.justice.services.healthcheck.utils.database.TableChecker;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.SystemIdMapperDataSourceProvider;

import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;

public class IdMapperViewstoreDatabaseHealthcheck implements Healthcheck {

    public static final String ID_MAPPER_VIEWSTORE_DATABASE_HEALTHCHECK_NAME = "system-id-mapper-viewstore-database-healthcheck";

    private static final List<String> ID_MAPPER_DATABASE_TABLE_NAMES = of("mapping");

    @Inject
    private SystemIdMapperDataSourceProvider systemJdbcDataSourceProvider;

    @Inject
    private TableChecker tableChecker;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Override
    public String getHealthcheckName() {
        return ID_MAPPER_VIEWSTORE_DATABASE_HEALTHCHECK_NAME;
    }

    @Override
    public String healthcheckDescription() {
        return "Checks connectivity to the system-id-mapper viewstore database and that all its tables are available";
    }

    @Override
    public HealthcheckResult runHealthcheck() {

        final DataSource jobStoreDataSource = systemJdbcDataSourceProvider.getDataSource();

        try {
            return tableChecker.checkTables(ID_MAPPER_DATABASE_TABLE_NAMES, jobStoreDataSource);

        } catch (final SQLException e) {
            logger.error("Healthcheck for system-id-mapper viewstore database failed.", e);
            return failure(format("Exception thrown accessing system-id-mapper viewstore database. %s: %s", e.getClass().getName(), e.getMessage()));
        }
    }
}
