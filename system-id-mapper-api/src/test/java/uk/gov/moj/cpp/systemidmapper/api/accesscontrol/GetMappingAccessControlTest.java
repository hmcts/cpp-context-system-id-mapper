package uk.gov.moj.cpp.systemidmapper.api.accesscontrol;


import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class GetMappingAccessControlTest extends BaseDroolsAccessControlTest {

    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public GetMappingAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @AfterEach
    public void tearDown() {
        verify(userAndGroupProvider).isSystemUser(action);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetMapping() {

        action = createActionFor("systemid.get-mapping");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToGetMapping() {

        action = createActionFor("systemid.get-mapping");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldAllowAuthorisedUserToCreateMapping() {

        action = createActionFor("systemid.map");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToCreateMapping() {

        action = createActionFor("systemid.map");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldAllowAuthorisedUserToRemapMapping() {

        action = createActionFor("systemid.remap");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToRemapMapping() {

        action = createActionFor("systemid.remap");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldAllowAuthorisedUserToCreateMappings() {

        action = createActionFor("systemid.map.list");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToCreateMappings() {

        action = createActionFor("systemid.map.list");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldAllowAuthorisedUserToBulkMappings() {

        action = createActionFor("systemid.find-mappings-bulk");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToBulkMappings() {

        action = createActionFor("systemid.find-mappings-bulk");
        when(userAndGroupProvider.isSystemUser(action)).thenReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }
}
