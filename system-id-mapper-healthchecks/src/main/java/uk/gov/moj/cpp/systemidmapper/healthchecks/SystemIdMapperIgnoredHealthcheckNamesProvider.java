package uk.gov.moj.cpp.systemidmapper.healthchecks;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.healthcheck.healthchecks.EventStoreHealthcheck.EVENT_STORE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.FileStoreHealthcheck.FILE_STORE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.SystemDatabaseHealthcheck.SYSTEM_DATABASE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.ViewStoreHealthcheck.VIEW_STORE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.artemis.ArtemisHealthcheck.ARTEMIS_HEALTHCHECK_NAME;

import uk.gov.justice.services.healthcheck.api.DefaultIgnoredHealthcheckNamesProvider;

import java.util.List;

import jakarta.enterprise.inject.Specializes;

@Specializes
public class SystemIdMapperIgnoredHealthcheckNamesProvider extends DefaultIgnoredHealthcheckNamesProvider {

    public SystemIdMapperIgnoredHealthcheckNamesProvider() {
        // This constructor is required by CDI.
    }

    @Override
    public List<String> getNamesOfIgnoredHealthChecks() {
        return asList(
                EVENT_STORE_HEALTHCHECK_NAME,
                FILE_STORE_HEALTHCHECK_NAME,
                JOB_STORE_HEALTHCHECK_NAME,
                SYSTEM_DATABASE_HEALTHCHECK_NAME,
                VIEW_STORE_HEALTHCHECK_NAME,
                ARTEMIS_HEALTHCHECK_NAME
        );
    }
}