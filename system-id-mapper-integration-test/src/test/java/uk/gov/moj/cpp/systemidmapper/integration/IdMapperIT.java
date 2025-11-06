package uk.gov.moj.cpp.systemidmapper.integration;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.systemid.mapper.SystemIdMappings;
import uk.gov.justice.systemid.mapper.SystemidMappingList;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests using manually created REST calls.
 */
public class IdMapperIT {

    private static final String URL = "http://" + getHost() + ":8080/system-id-mapper-api/rest/systemid/mappings/";
    private static final String BULK_URL = "http://" + getHost() + ":8080/system-id-mapper-api/rest/systemid/mappings/bulk";

    private final DatabaseSeeder databaseSeeder = new DatabaseSeeder();

    private static final UUID USER_ID = fromString("bb593957-08a8-4d41-a5c1-7674d38d4f43");

    private final RestClient restClient = new RestClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final UsersAndGroupsWiremockStub usersAndGroupsWiremockStub = new UsersAndGroupsWiremockStub();

    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private static final String QUERY_MEDIA_TYPE = "application/vnd.systemid.mapping+json";
    private static final String QUERY_BULK_MEDIA_TYPE = "application/vnd.systemid.mappings+json";

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

        final String requestPayload = createObjectBuilder()
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetId", targetId.toString())
                .add("targetType", targetType)
                .build().toString();

        final Response response = restClient.postCommand(URL, "application/vnd.systemid.map+json", requestPayload, headers());
        final String mappingId = stringToJsonObjectConverter.convert(response.readEntity(String.class)).getString("id");

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        assertThat(mappingId, is(notNullValue()));

        // Retrieve mapping
        final RequestParams requestParams = requestParams(URL + mappingId, QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String responsePayload = poll(requestParams).until(status().is(OK)).getPayload();

        with(responsePayload)
                .assertThat("$.mappingId", is(mappingId))
                .assertThat("$.sourceId", is(sourceId))
                .assertThat("$.sourceType", is("TFL ID"))
                .assertThat("$.targetId", is(targetId.toString()))
                .assertThat("$.targetType", is("CASE ID"))
        ;
    }

    @Test
    public void shouldGetConflictOnInsertNewMapping() throws Exception {
        final UUID mappingId = randomUUID();
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        databaseSeeder.insertMapping(
                mappingId,
                sourceId,
                sourceType,
                targetId,
                targetType
        );

        final UUID differentTargetId = randomUUID();
        final String requestPayload = createObjectBuilder()
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetId", differentTargetId.toString())
                .add("targetType", targetType)
                .build().toString();

        final Response response = new RestClient().postCommand(URL, "application/vnd.systemid.map+json", requestPayload, headers());
        final String conflictedMappingId = new StringToJsonObjectConverter().convert(response.readEntity(String.class)).getString("id");

        assertThat(response.getStatus(), is(CONFLICT.getStatusCode()));
        assertThat(conflictedMappingId, is(equalTo(mappingId.toString())));
    }

    @Test
    public void shouldRetrieveAMappingById() throws Exception {

        final UUID mappingId = randomUUID();
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        databaseSeeder.insertMapping(
                mappingId,
                sourceId,
                sourceType,
                targetId,
                targetType
        );

        final String url = URL + mappingId;
        final RequestParams requestParams = requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String payload = poll(requestParams).until(status().is(OK)).getPayload();

        with(payload)
                .assertThat("$.mappingId", is(mappingId.toString()))
                .assertThat("$.sourceId", is(sourceId))
                .assertThat("$.sourceType", is("TFL ID"))
                .assertThat("$.targetId", is(targetId.toString()))
                .assertThat("$.targetType", is("CASE ID"))
        ;
    }

    @Test
    public void shouldRetrieveAMappingSourceAndTargetType() throws Exception {

        final UUID mappingId = randomUUID();
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        databaseSeeder.insertMapping(
                mappingId,
                sourceId,
                sourceType,
                targetId,
                targetType
        );

        final String url = URL + format("?sourceId=%s&sourceType=%s&targetType=%s", sourceId, sourceType, targetType);
        final RequestParams requestParams = requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String payload = poll(requestParams).until(status().is(OK)).getPayload();

        with(payload)
                .assertThat("$.mappingId", is(mappingId.toString()))
                .assertThat("$.sourceId", is(sourceId))
                .assertThat("$.sourceType", is("TFL ID"))
                .assertThat("$.targetId", is(targetId.toString()))
                .assertThat("$.targetType", is("CASE ID"))
        ;
    }

    @Test
    public void shouldRetrieveAMappingTargetIdAndType() throws Exception {

        final UUID mappingId = randomUUID();
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        databaseSeeder.insertMapping(
                mappingId,
                sourceId,
                sourceType,
                targetId,
                targetType
        );

        final String url = URL + format("?targetId=%s&targetType=%s", targetId, targetType);
        final RequestParams requestParams = requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String payload = poll(requestParams).until(status().is(OK)).getPayload();

        with(payload)
                .assertThat("$.mappingId", is(mappingId.toString()))
                .assertThat("$.sourceId", is(sourceId))
                .assertThat("$.sourceType", is("TFL ID"))
                .assertThat("$.targetId", is(targetId.toString()))
                .assertThat("$.targetType", is("CASE ID"))
        ;
    }

    @Test
    public void shouldRespondWith404NotFoundOnInvalidGetMapping() throws Exception {

        final UUID nonExistingId = randomUUID();
        final String url = URL + nonExistingId;

        final Response response = new RestClient().query(url, QUERY_MEDIA_TYPE, headers());

        assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));
    }

    @Test
    public void shouldRespondWith404NotFoundOnInvalidFindMapping() throws Exception {

        final UUID nonExistingId = randomUUID();
        final String targetType = "Gerritt";
        final String url = URL + format("?targetId=%s&targetType=%s", nonExistingId, targetType);

        final Response response = new RestClient().query(url, QUERY_MEDIA_TYPE, headers());

        assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));
    }

    @Test
    public void shouldRespondWith400BadRequestOnInvalidQueryParameters() throws Exception {

        final String targetType = "Gerritt";
        final String url = URL + format("?targetType=%s", targetType);

        final Response response = new RestClient().query(url, QUERY_MEDIA_TYPE, headers());

        assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
    }

    private MultivaluedHashMap<String, Object> headers() {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, USER_ID.toString());
        return headers;
    }

    @Test
    public void shouldRetrieveAMappingSourceIdAndTargetType() throws Exception {

        final UUID mappingId = randomUUID();
        final String sourceId = "GAFTL00:C2AAACD3455";
        final String sourceType = "MCC-REF";
        final UUID targetId = randomUUID();
        final String targetType = "CASE-ID";

        databaseSeeder.insertMapping(
                mappingId,
                sourceId,
                sourceType,
                targetId,
                targetType
        );

        final String url = URL + format("?sourceId=%s&targetType=%s", sourceId, targetType);

        final RequestParams requestParams = requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String payload = poll(requestParams).until(status().is(OK)).getPayload();

        with(payload)
                .assertThat("$.mappingId", is(mappingId.toString()))
                .assertThat("$.sourceId", is(sourceId))
                .assertThat("$.sourceType", is(sourceType))
                .assertThat("$.targetId", is(targetId.toString()))
                .assertThat("$.targetType", is(targetType))
        ;
    }

    @Test
    public void shouldRemapSourceId() {

        // Insert a mapping
        final String sourceId = "original-source-id";
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final String insertPayload = createObjectBuilder()
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetId", targetId.toString())
                .add("targetType", targetType)
                .build().toString();

        final Response insertResponse = restClient.postCommand(URL, "application/vnd.systemid.map+json", insertPayload, headers());
        final String mappingId = stringToJsonObjectConverter.convert(insertResponse.readEntity(String.class)).getString("id");

        assertThat(insertResponse.getStatus(), is(OK.getStatusCode()));
        assertThat(mappingId, is(notNullValue()));

        // Update the sourceId
        final String newSourceId = "new-source-id";
        final String remapPayload = createObjectBuilder()
                .add("newSourceId", newSourceId)
                .add("mappingId", mappingId)
                .build().toString();

        final Response remapResponse = restClient.postCommand(URL, "application/vnd.systemid.remap+json", remapPayload, headers());
        assertThat(remapResponse.getStatus(), is(OK.getStatusCode()));

        final String remapResponsePayload = remapResponse.readEntity(String.class);

        with(remapResponsePayload)
                .assertThat("$.mappingId", is(mappingId))
                .assertThat("$.sourceId", is(newSourceId))
                .assertThat("$.sourceType", is("TFL ID"))
                .assertThat("$.targetId", is(targetId.toString()))
                .assertThat("$.targetType", is("CASE ID"))
        ;


        // Retrieve mapping
        final RequestParams requestParams = requestParams(URL + mappingId, QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String responsePayload = poll(requestParams).until(status().is(OK)).getPayload();

        with(responsePayload)
                .assertThat("$.mappingId", is(mappingId))
                .assertThat("$.sourceId", is(newSourceId))
                .assertThat("$.sourceType", is("TFL ID"))
                .assertThat("$.targetId", is(targetId.toString()))
                .assertThat("$.targetType", is("CASE ID"))
        ;
    }

    @Test
    public void shouldInsertNewMappings() {
        final JsonObject systemIdMapListJson = FileUtil.givenPayload("/test-data/systemid.map.list.json");
        final Response response = restClient.postCommand(URL, "application/vnd.systemid.map.list+json", systemIdMapListJson.toString(), headers());
        final JsonObject jsonObjectPostResponse = stringToJsonObjectConverter.convert(response.readEntity(String.class));
        SystemidMappingList systemidMappingList = jsonObjectConverter.convert(jsonObjectPostResponse, SystemidMappingList.class);

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        assertThat(systemidMappingList.getSystemIdMappings().size(), is(2));

        final SystemIdMappings systemIdMapping1 = systemidMappingList.getSystemIdMappings().get(0);

        // Retrieve mapping
        String responsePayload = poll(requestParams(URL + systemIdMapping1.getMappingId(), QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build()).until(status().is(OK)).getPayload();
        assertThat(responsePayload, notNullValue());
        with(responsePayload)
                .assertThat("$.mappingId", is(systemIdMapping1.getMappingId().toString()))
                .assertThat("$.sourceId", is(systemIdMapping1.getSourceId()))
                .assertThat("$.sourceType", notNullValue())
                .assertThat("$.targetId", is(systemIdMapping1.getTargetId().toString()))
                .assertThat("$.targetType", notNullValue())
        ;

        final SystemIdMappings systemIdMapping2 = systemidMappingList.getSystemIdMappings().get(1);

        responsePayload = poll(requestParams(URL + systemIdMapping2.getMappingId(), QUERY_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build()).until(status().is(OK)).getPayload();
        assertThat(responsePayload, notNullValue());
        with(responsePayload)
                .assertThat("$.mappingId", is(systemIdMapping2.getMappingId().toString()))
                .assertThat("$.sourceId", is(systemIdMapping2.getSourceId()))
                .assertThat("$.sourceType", notNullValue())
                .assertThat("$.targetId", is(systemIdMapping2.getTargetId().toString()))
                .assertThat("$.targetType", notNullValue())
        ;

        final JsonObject systemIdConflictTargetIdMapListJson = FileUtil.givenPayload("/test-data/systemid.map.list-conflict.json");
        final Response conflictTargetIdResponse = restClient.postCommand(URL, "application/vnd.systemid.map.list+json", systemIdConflictTargetIdMapListJson.toString(), headers());
        SystemidMappingList systemIdMappingList = jsonObjectConverter.convert(
                stringToJsonObjectConverter.convert(conflictTargetIdResponse.readEntity(String.class))
                , SystemidMappingList.class);

        assertThat(conflictTargetIdResponse.getStatus(), is(OK.getStatusCode()));
        assertThat(systemIdMappingList.getSystemIdMappings().size(), is(1));
        assertThat(systemIdMappingList.getSystemIdMappings().get(0).getTargetId().toString(), is(systemIdMapping2.getTargetId().toString()));
        assertThat(systemIdMappingList.getSystemIdMappings().get(0).getIsError(), is(false));
    }

    @Test
    public void shouldInsertNewMappingBulkForSourceIdsAndTargetType() {
        final String sourceId1 = "sourceId1";
        final String sourceId2 = "sourceId2";
        final String sourceIds = "sourceId1,sourceId2";
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final String requestPayload1 = createObjectBuilder()
                .add("sourceId", sourceId1)
                .add("sourceType", sourceType)
                .add("targetId", targetId.toString())
                .add("targetType", targetType)
                .build().toString();

        final String requestPayload2 = createObjectBuilder()
                .add("sourceId", sourceId2)
                .add("sourceType", sourceType)
                .add("targetId", targetId.toString())
                .add("targetType", targetType)
                .build().toString();

        final Response response1 = restClient.postCommand(URL, "application/vnd.systemid.map+json", requestPayload1, headers());
        final Response response2 = restClient.postCommand(URL, "application/vnd.systemid.map+json", requestPayload2, headers());

        assertThat(response1.getStatus(), is(OK.getStatusCode()));
        assertThat(response2.getStatus(), is(OK.getStatusCode()));

        // Retrieve mapping
        final String url = BULK_URL + format("?sourceIds=%s&targetType=%s", sourceIds, targetType);
        final RequestParams requestParams = requestParams(url , QUERY_BULK_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String responsePayload = poll(requestParams).until(status().is(OK)).getPayload();

        with(responsePayload)
                .assertThat("$.systemIds[0].sourceId", is(sourceId1))
                .assertThat("$.systemIds[1].sourceId", is(sourceId2))
                .assertThat("$.systemIds[0].sourceType", is("TFL ID"))
                .assertThat("$.systemIds[0].targetType", is("CASE ID"))
                .assertThat("$.systemIds[1].targetType", is("CASE ID"))
        ;
    }

    @Test
    public void shouldInsertNewMappingBulkForTargetIdsAndTargetType() {

        final String sourceId1 = "sourceId1";
        final String sourceId2 = "sourceId2";
        final String sourceType = "TFL ID";
        final UUID targetId1 = randomUUID();
        final UUID targetId2 = randomUUID();
        String strTargetId1 = targetId1.toString();
        String strTargetId2 = targetId2.toString();
        final String targetType = "CASE ID";
        String targetIds = String.join(",", strTargetId1, strTargetId2);

        final String requestPayload1 = createObjectBuilder()
                .add("sourceId", sourceId1)
                .add("sourceType", sourceType)
                .add("targetId", strTargetId1)
                .add("targetType", targetType)
                .build().toString();

        final String requestPayload2 = createObjectBuilder()
                .add("sourceId", sourceId2)
                .add("sourceType", sourceType)
                .add("targetId", strTargetId2)
                .add("targetType", targetType)
                .build().toString();

        final Response response1 = restClient.postCommand(URL, "application/vnd.systemid.map+json", requestPayload1, headers());
        final Response response2 = restClient.postCommand(URL, "application/vnd.systemid.map+json", requestPayload2, headers());

        assertThat(response1.getStatus(), is(OK.getStatusCode()));
        assertThat(response2.getStatus(), is(OK.getStatusCode()));

        // Retrieve mapping
        final String url = BULK_URL + format("?targetIds=%s&targetType=%s", targetIds, targetType);
        final RequestParams requestParams = requestParams(url , QUERY_BULK_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        final String responsePayload = poll(requestParams).until(status().is(OK)).getPayload();

        with(responsePayload)
                .assertThat("$.systemIds[0].targetId", is(strTargetId1))
                .assertThat("$.systemIds[1].targetId", is(strTargetId2))
                .assertThat("$.systemIds[0].sourceType", is("TFL ID"))
                .assertThat("$.systemIds[0].targetType", is("CASE ID"))
                .assertThat("$.systemIds[1].targetType", is("CASE ID"))
        ;
    }

}
