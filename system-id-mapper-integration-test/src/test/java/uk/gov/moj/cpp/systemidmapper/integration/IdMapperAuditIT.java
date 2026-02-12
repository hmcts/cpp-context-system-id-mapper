package uk.gov.moj.cpp.systemidmapper.integration;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for auditing.
 */
@ExtendWith(JmsResourceManagementExtension.class)
class IdMapperAuditIT {

    private static final String URL = "http://" + getHost() + ":8080/system-id-mapper-api/rest/systemid/mappings/";
    private static final String APP_NAME = "system-id-mapper-api";
    private static final String INSERT_ACTION_NAME = "systemid.map";
    private static final String GET_ACTION_NAME = "systemid.get-mapping";

    private final DatabaseSeeder databaseSeeder = new DatabaseSeeder();
    private final JmsMessageConsumerClient auditMessageConsumerClient = JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider("auditing")
            .withEventNames("audit.events.audit-recorded")
            .getMessageConsumerClient();

    private static final UUID USER_ID = fromString("bb593957-08a8-4d41-a5c1-7674d38d4f43");
    private final UsersAndGroupsWiremockStub usersAndGroupsWiremockStub = new UsersAndGroupsWiremockStub();

    @BeforeEach
    public void setup() throws Exception {
        databaseSeeder.cleanTables();
    }

    @BeforeEach
    public void stubUsersAndGroups() {
        usersAndGroupsWiremockStub.stubIsSystemUserCallFor(USER_ID);
    }

    @Test
    void shouldAuditInsertNewMapping() {
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final String requestPayload = JsonObjects.createObjectBuilder()
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetId", targetId.toString())
                .add("targetType", targetType)
                .build().toString();


        new RestClient().postCommand(
                URL,
                "application/vnd.systemid.map+json",
                requestPayload,
                headers());

        final String message = retrieveLatestMessage();

        assertThat(message, notNullValue());
        with(message)
                .assertThat("$.content", notNullValue())
                .assertThat("$.content.targetId", is(targetId.toString()))
                .assertThat("$.content._metadata.name", is(INSERT_ACTION_NAME))
                .assertThat("$.origin", is(APP_NAME));

        retrieveLatestMessage();
        verifyNoMoreMessagesAreOnTopic();
    }

    @Test
    void shouldAuditGetMapping() {
        final String sourceId = randomUUID().toString();
        final String sourceType = "TFL ID";
        final UUID targetId = randomUUID();
        final String targetType = "CASE ID";

        final String requestPayload = JsonObjects.createObjectBuilder()
                .add("sourceId", sourceId)
                .add("sourceType", sourceType)
                .add("targetId", targetId.toString())
                .add("targetType", targetType)
                .build().toString();

        final Response response = new RestClient().postCommand(
                URL,
                "application/vnd.systemid.map+json",
                requestPayload,
                headers());
        final String mappingId = new StringToJsonObjectConverter().convert(response.readEntity(String.class)).getString("id");

        final String mediaType = "application/vnd.systemid.mapping+json";
        final RequestParams requestParams = requestParams(URL + mappingId, mediaType)
                .withHeader(HeaderConstants.USER_ID, USER_ID)
                .build();

        auditMessageConsumerClient.clearMessages();
        poll(requestParams).until(status().is(OK)).getPayload();

        // Retrieve the audit get message
        final String getMessage = retrieveLatestMessage();

        assertThat(getMessage, notNullValue());

        with(getMessage)
                .assertThat("$.content", notNullValue())
                .assertThat("$.content.mappingId", is(mappingId))
                .assertThat("$.content._metadata.name", is(GET_ACTION_NAME))
                .assertThat("$.origin", is(APP_NAME));


        skipResponseMessageAndVerifyAuditQueueIsEmpty();
    }

    private void skipResponseMessageAndVerifyAuditQueueIsEmpty() {
        retrieveLatestMessage();
        verifyNoMoreMessagesAreOnTopic();
    }

    private void verifyNoMoreMessagesAreOnTopic() {
        assertThat(retrieveLatestMessage(), nullValue());
    }

    private String retrieveLatestMessage() {
        return auditMessageConsumerClient.retrieveMessageNoWait().orElse(null);
    }

    private MultivaluedHashMap<String, Object> headers() {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, USER_ID.toString());
        return headers;
    }
}
