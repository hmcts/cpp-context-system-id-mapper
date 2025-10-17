package uk.gov.moj.cpp.systemidmapper.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import uk.gov.justice.services.test.utils.persistence.AbstractJdbcRepositoryIT;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingConflictException;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemIdMappingJdbcRepositoryIT extends AbstractJdbcRepositoryIT<SystemIdMappingJdbcRepository> {

    private static final String LIQUIBASE_MAPPING_CHANGELOG_XML = "liquibase/id-mapper-changelog.xml";

    private SystemIdMappingJdbcRepository jdbcRepository;

    public SystemIdMappingJdbcRepositoryIT() {
        super(LIQUIBASE_MAPPING_CHANGELOG_XML);
    }

    @BeforeEach
    public void initializeDependencies() throws Exception {
        jdbcRepository = new SystemIdMappingJdbcRepository();
        registerDataSource();
    }

    @Test
    public void shouldInsertNewMappingAndReturnCreatedMappingId() throws MappingConflictException {
        final String sourceId = "sourceId";
        final String sourceType = "sourceType";
        final String targetType = "targetType";
        final UUID targetId = randomUUID();

        final UUID mappingId = jdbcRepository.insertSystemIdMapping(sourceId, sourceType, targetId, targetType);

        Optional<SystemIdMapping> systemIdMapping = jdbcRepository.getSystemIdMapping(mappingId);

        assertThat(systemIdMapping, is(notNullValue()));
    }
}