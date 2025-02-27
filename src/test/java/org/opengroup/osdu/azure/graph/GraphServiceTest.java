package org.opengroup.osdu.azure.graph;

import com.microsoft.graph.groups.GroupsRequestBuilder;
import com.microsoft.graph.groups.item.GroupItemRequestBuilder;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.models.ServicePrincipalCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.serviceprincipals.ServicePrincipalsRequestBuilder;
import com.microsoft.graph.serviceprincipals.item.ServicePrincipalItemRequestBuilder;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphServiceTest {

    @InjectMocks
    GraphService sut;

    @Mock
    private GraphServiceClientFactory graphServiceClientFactory;

    @Mock
    private GraphServiceClient graphServiceClient;

    @Test
    void isOidValid_returnsTrue_ifTokenIssuerIsAAD_andOidIsValidUser() {
        User userInfo = new User();
        userInfo.setGivenName("userName");
        UserItemRequestBuilder response = mock(UserItemRequestBuilder.class);

        when(response.get()).thenReturn(userInfo);
        when(graphServiceClientFactory.getGraphServiceClient("data-partition")).thenReturn(graphServiceClient);
        UsersRequestBuilder usersRequestBuilder = mock(UsersRequestBuilder.class);
        when(usersRequestBuilder.byUserId("oid")).thenReturn(response);

        when(graphServiceClient.users()).thenReturn(usersRequestBuilder);

        assertTrue(sut.isOidValid("data-partition", "oid"));
    }

    @Test
    void isOidValid_returnsFalse_ifDataPartitionIsEmpty() {
        assertFalse(sut.isOidValid("", "oid"));
    }

    @Test
    void isOidValid_returnsFalse_ifOidIsEmpty() {
        assertFalse(sut.isOidValid("data-partition", ""));
    }

    @Test
    void isOidValid_throwsBadRequestException_ifOIDIsValidPrincipalServiceOid() {
        ServicePrincipal userInfo = new ServicePrincipal();
        userInfo.setDisplayName("service-principal");
        ServicePrincipalItemRequestBuilder response = mock(ServicePrincipalItemRequestBuilder.class);

        when(response.get()).thenReturn(userInfo);
        when(graphServiceClientFactory.getGraphServiceClient("data-partition")).thenReturn(graphServiceClient);
        ServicePrincipalsRequestBuilder servicePrincipalsRequestBuilder = mock(ServicePrincipalsRequestBuilder.class);
        when(servicePrincipalsRequestBuilder.byServicePrincipalId("oid")).thenReturn(response);

        when(graphServiceClient.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilder);

        AppException exception = assertThrows(AppException.class, () -> sut.isOidValid("data-partition", "oid"));

        assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getError().getCode());
        assertEquals("The given OID matches with a provisioned Service Principal. They should be added to OSDU groups via their Client ID. Please use the correct ID as the input", exception.getMessage());
    }

    @Test
    void isOidValid_returnsTrue_ifClientIdIsValidPrincipalServiceClientId() {
        ServicePrincipal userInfo = new ServicePrincipal();
        userInfo.setDisplayName("service-principal");

        ServicePrincipalCollectionResponse servicePrincipalCollectionResponse = new ServicePrincipalCollectionResponse();
        servicePrincipalCollectionResponse.setValue(Collections.singletonList(userInfo));

        ServicePrincipalsRequestBuilder requestBuilder = mock(ServicePrincipalsRequestBuilder.class);

        when(requestBuilder.get(any())).thenReturn(servicePrincipalCollectionResponse);

        when(graphServiceClientFactory.getGraphServiceClient("data-partition")).thenReturn(graphServiceClient);

        when(graphServiceClient.servicePrincipals()).thenReturn(requestBuilder);

        assertTrue(sut.isOidValid("data-partition", "clientId"));
    }

    @Test
    void isOidValid_returnsTrue_ifClientIdIsValidAadGroupId() {
        Group group = new Group();
        group.setDisplayName("group-name");

        GroupItemRequestBuilder groupItemRequestBuilder = mock(GroupItemRequestBuilder.class);
        when(groupItemRequestBuilder.get()).thenReturn(group);

        GroupsRequestBuilder requestBuilder = mock(GroupsRequestBuilder.class);
        when(requestBuilder.byGroupId("oId")).thenReturn(groupItemRequestBuilder);

        when(graphServiceClientFactory.getGraphServiceClient("data-partition")).thenReturn(graphServiceClient);

        when(graphServiceClient.groups()).thenReturn(requestBuilder);

        assertTrue(sut.isOidValid("data-partition", "oId"));
    }
}