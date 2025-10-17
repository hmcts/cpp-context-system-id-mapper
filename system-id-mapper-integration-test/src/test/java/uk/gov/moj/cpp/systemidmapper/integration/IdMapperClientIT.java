package uk.gov.moj.cpp.systemidmapper.integration;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.OK;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.DefaultSystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.MappingNotFoundException;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests using the {@link DefaultSystemIdMapperClient} to construct REST calls.
 */
public class IdMapperClientIT {

    private static final String URL = "http://" + getHost() + ":8080/system-id-mapper-api/rest/systemid/";

    private final DatabaseSeeder databaseSeeder = new DatabaseSeeder();
    private static final UUID USER_ID = fromString("bb593957-08a8-4d41-a5c1-7674d38d4f43");
    private final UsersAndGroupsWiremockStub usersAndGroupsWiremockStub = new UsersAndGroupsWiremockStub();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @BeforeEach
    public void cleanTheDatabase() throws Exception {
        databaseSeeder.cleanTables();
    }

    @BeforeEach
    public void stubUsersAndGroups() {
        usersAndGroupsWiremockStub.stubIsSystemUserCallFor(USER_ID);
    }

    @Test
    public void shouldInsertNewMapping() {
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);
        final SystemIdMap systemIdMap = new SystemIdMap(sourceId, sourceType, targetId, targetType);
        final AdditionResponse response = client.add(systemIdMap, USER_ID);

        assertThat(response.code(), is(OK));
        assertThat(response.mappingId(), is(notNullValue()));

        // Retrieve mapping
        final SystemIdMapping foundMapping = client.getMappingBy(response.mappingId(), USER_ID);

        assertThat(foundMapping.getMappingId(), is(equalTo(response.mappingId())));
        assertThat(foundMapping.getSourceId(), is(equalTo(sourceId)));
        assertThat(foundMapping.getSourceType(), is(equalTo(sourceType)));
        assertThat(foundMapping.getTargetId(), is(equalTo(targetId)));
        assertThat(foundMapping.getTargetType(), is(equalTo(targetType)));
        assertThat(foundMapping.getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void shouldGetConflictOnInsertNewMapping() throws Exception {
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);
        final SystemIdMap systemIdMap = new SystemIdMap(sourceId, sourceType, targetId, targetType);
        final AdditionResponse response = client.add(systemIdMap, USER_ID);

        assertThat(response.code(), is(OK));
        assertThat(response.mappingId(), is(notNullValue()));

        final UUID differentTargetId = randomUUID();

        final AdditionResponse conflictedResponse = client.add(new SystemIdMap(sourceId, sourceType, differentTargetId, targetType), USER_ID);

        assertThat(conflictedResponse.code(), is(ResultCode.CONFLICT));
    }

    @Test
    public void shouldRetrieveAMappingSourceAndTargetType() throws Exception {

        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);

        final SystemIdMap systemIdMap = new SystemIdMap(sourceId, sourceType, targetId, targetType);
        final AdditionResponse response = client.add(systemIdMap, USER_ID);

        assertThat(response.code(), is(OK));
        assertThat(response.mappingId(), is(notNullValue()));

        // Retrieve mapping
        final SystemIdMapping foundMapping = client.findBy(sourceId, sourceType, targetType, USER_ID)
                .orElseThrow(() -> new AssertionError("Failed to find mapping"));

        assertThat(foundMapping.getMappingId(), is(equalTo(response.mappingId())));
        assertThat(foundMapping.getSourceId(), is(equalTo(sourceId)));
        assertThat(foundMapping.getSourceType(), is(equalTo(sourceType)));
        assertThat(foundMapping.getTargetId(), is(equalTo(targetId)));
        assertThat(foundMapping.getTargetType(), is(equalTo(targetType)));
        assertThat(foundMapping.getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void shouldRetrieveAMappingTargetIdAndType() throws Exception {

        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);
        final SystemIdMap systemIdMap = new SystemIdMap(sourceId, sourceType, targetId, targetType);
        final AdditionResponse response = client.add(systemIdMap, USER_ID);


        assertThat(response.code(), is(OK));
        assertThat(response.mappingId(), is(notNullValue()));

        // Retrieve mapping
        final SystemIdMapping foundMapping = client.findBy(targetId, targetType, USER_ID)
                .orElseThrow(() -> new AssertionError("Failed to find mapping"));

        assertThat(foundMapping.getMappingId(), is(equalTo(response.mappingId())));
        assertThat(foundMapping.getSourceId(), is(equalTo(sourceId)));
        assertThat(foundMapping.getSourceType(), is(equalTo(sourceType)));
        assertThat(foundMapping.getTargetId(), is(equalTo(targetId)));
        assertThat(foundMapping.getTargetType(), is(equalTo(targetType)));
        assertThat(foundMapping.getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void shouldThrowAMappingNotFoundExceptionIfGetMappingHasNoMatch() throws Exception {

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);

        final UUID unknownMappingId = randomUUID();
        try {
            client.getMappingBy(unknownMappingId, USER_ID);
            fail();
        } catch (final MappingNotFoundException expected) {
            assertThat(expected.getMessage(), is(format("Failed to find mapping for id %s", unknownMappingId)));
        }
    }

    @Test
    public void shouldReturnEmptyOnInvalidFindMappingBySource() throws Exception {

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);

        final Optional<SystemIdMapping> systemIdMapping = client.findBy(
                "unknown source id",
                "unknown source type",
                "unknown target type",
                USER_ID);

        assertThat(systemIdMapping.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyOnInvalidFindMappingByType() throws Exception {

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);

        final Optional<SystemIdMapping> systemIdMapping = client.findBy(
                randomUUID(),
                "unknown target type",
                USER_ID);

        assertThat(systemIdMapping.isPresent(), is(false));
    }


    @Test
    public void shouldRetrieveAMappingForSourceIdAndTargetType() throws Exception {

        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType1 = "CASE-ID";
        final String targetType2 = "CASE-FILE-ID";

        final DefaultSystemIdMapperClient client = new DefaultSystemIdMapperClient(URL, objectMapper);

        final SystemIdMap systemIdMap = new SystemIdMap(sourceId, sourceType, targetId, targetType1);
        final AdditionResponse response = client.add(systemIdMap, USER_ID);

        assertThat(response.code(), is(OK));
        assertThat(response.mappingId(), is(notNullValue()));

        // Retrieve mapping
        final SystemIdMapping foundMapping = client.findBy(USER_ID,sourceId,targetType1,targetType2)
                .orElseThrow(() -> new AssertionError("Failed to find mapping"));

        assertThat(foundMapping.getMappingId(), is(equalTo(response.mappingId())));
        assertThat(foundMapping.getSourceId(), is(equalTo(sourceId)));
        assertThat(foundMapping.getSourceType(), is(equalTo(sourceType)));
        assertThat(foundMapping.getTargetId(), is(equalTo(targetId)));
        assertThat(foundMapping.getTargetType(), is(equalTo(targetType1)));
        assertThat(foundMapping.getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void shouldRemapTheSourceIdOfAnExistingMapping() throws Exception {

        // add original mapping
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final SystemIdMapperClient systemIdMapperClient = new DefaultSystemIdMapperClient(URL, objectMapper);
        final SystemIdMap systemIdMap = new SystemIdMap(sourceId, sourceType, targetId, targetType);
        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, USER_ID);

        assertThat(response.code(), is(OK));
        assertThat(response.mappingId(), is(notNullValue()));

        final UUID mappingId = response.mappingId();
        final String newSourceId = "new-source-id";

        final Optional<SystemIdMapping> systemIdMapping = systemIdMapperClient.remap(newSourceId, mappingId, USER_ID);

        if (! systemIdMapping.isPresent()) {
            fail();
        }

        assertThat(systemIdMapping.get().getSourceId(), is(newSourceId));
        assertThat(systemIdMapping.get().getSourceType(), is(sourceType));
        assertThat(systemIdMapping.get().getMappingId(), is(mappingId));
        assertThat(systemIdMapping.get().getTargetId(), is(targetId));
        assertThat(systemIdMapping.get().getTargetType(), is(targetType));

        final SystemIdMapping storedSystemIdMapping = systemIdMapperClient.getMappingBy(mappingId, USER_ID);

        assertThat(storedSystemIdMapping.getSourceId(), is(newSourceId));
        assertThat(storedSystemIdMapping.getSourceType(), is(sourceType));
        assertThat(storedSystemIdMapping.getMappingId(), is(mappingId));
        assertThat(storedSystemIdMapping.getTargetId(), is(targetId));
        assertThat(storedSystemIdMapping.getTargetType(), is(targetType));
    }
}
