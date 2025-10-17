package uk.gov.moj.cpp.systemidmapper.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.http.RequestParams;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Standalone utility for subbing the ping to Users and Groups to identify if the supplied userId
 * is a System user.
 *
 * To use:
 *
 * <pre>
 *   private static final UUID USER_ID = fromString("bb593957-08a8-4d41-a5c1-7674d38d4f43");
 *   private final UsersAndGroupsWiremockStub usersAndGroupsWiremockStub = new UsersAndGroupsWiremockStub();
 *
 *   &#064;Before
 *   public void stubUsersAndGroups() {
 *      usersAndGroupsWiremockStub.stubIsSystemUserCallFor(USER_ID);
 *   }
 * </pre>
 *
 *
 */
public class UsersAndGroupsWiremockStub {

    private static final String GROUP_NAME = encode("System Users");
    private static final String USERS_GROUPS_PATH =
            "/usersgroups-service/query/api/rest/" +
                    "usersgroups/users/%s/groups";

    private static final String MIME_TYPE = "application/vnd.usersgroups.groups+json";

    private static final String HOST = getHost();
    private static final int PORT = 8080;
    private static final String USERS_GROUPS_BASE_URL = "http://" + HOST + ":" + PORT;

    /**
     * The response body that will be returned by wiremock for the {@code usersgroups.get-groups-by-user}
     * call. Please note that the ids in this response don't matter - all that is checked
     * is that an array with some values (i.e. not empty) is returned.
     */
    private static final String USERS_GROUPS_RESPONSE_BODY =
            "{" +
                    "  \"groups\": [" +
                    "    {" +
                    "      \"groupId\": \"1e2f843e-d639-40b3-8611-8015f3a18958\"," +
                    "      \"groupName\": \"System Users\"" +
                    "    }," +
                    "    {" +
                    "      \"groupId\": \"5b6f843e-d639-40b3-8611-8015f3a34758\"," +
                    "      \"groupName\": \"Charging Users\"" +
                    "    }" +
                    "  ]" +
                    "}";


    /**
     * Stubs the call to {@code usersgroups.get-groups-by-user} for the group 'System Users' using
     * wiremock.
     *
     * @param userId the id of the user with system access control
     */
    public void stubIsSystemUserCallFor(final UUID userId) {

        configureFor(HOST, PORT);
        reset();

        stubPingFor("usersgroups-service");

        final String url = format(USERS_GROUPS_PATH, userId);
        stubFor(get(urlPathMatching(url))
                .withHeader("Accept", equalTo(MIME_TYPE))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("Content-Type", MIME_TYPE)
                        .withHeader(ID, randomUUID().toString())
                        .withBody(USERS_GROUPS_RESPONSE_BODY)));


        pollUntilRespondingCorrectly(userId);
    }

    private void pollUntilRespondingCorrectly(final UUID userId) {

        final String path = format(USERS_GROUPS_PATH, userId);
        final String url = USERS_GROUPS_BASE_URL + path;
        final RequestParams requestParams = requestParams(
                url,
                MIME_TYPE)
                .build();

        poll(requestParams).until(status().is(OK));
    }

    private static String encode(final String s) {
        final String encoding = UTF_8.name();
        try {
            return URLEncoder.encode(s, encoding);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to url encode using " + encoding);
        }
    }
}
