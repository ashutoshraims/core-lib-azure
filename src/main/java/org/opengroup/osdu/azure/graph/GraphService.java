package org.opengroup.osdu.azure.graph;

import com.microsoft.graph.groups.item.GroupItemRequestBuilder;
import com.microsoft.graph.models.ServicePrincipalCollectionResponse;
import com.microsoft.graph.serviceprincipals.item.ServicePrincipalItemRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.opengroup.osdu.common.Validators;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/***
 * Simple class to get name via OID from Graph Db.
 */
@Component
@Slf4j
public class GraphService {
    @Autowired
    private GraphServiceClientFactory graphServiceClientFactory;

    private static final String FIELD_NAME = "email/OID";
    private static final String EXCEPTION_WHILE = "Exception while %s, exception message: %s";

    /**
     * Returns true or false depending upon if the OID is a service principal OID.
     *
     * @param dataPartitionId data partition id
     * @param oId             object id
     * @return boolean Whether it's a service principal account or not
     */
    private boolean isPrincipalServiceOidValid(final String dataPartitionId, final String oId) {
        boolean isPrincipalServiceOid = false;
        try {
            Validators.checkNotNullAndNotEmpty(oId, FIELD_NAME);
            ServicePrincipalItemRequestBuilder servicePrincipalItemRequestBuilder = graphServiceClientFactory.
                    getGraphServiceClient(dataPartitionId).
                    servicePrincipals().
                    byServicePrincipalId(oId);

            String userName = servicePrincipalItemRequestBuilder.get().getDisplayName();
            log.info(String.format("Service principal OID: %s validated", userName));
            isPrincipalServiceOid = true;
        } catch (Exception exception) {
            log.info(String.format(EXCEPTION_WHILE, "Fetching the principal service by oid: " + oId, exception.getMessage()));
        }
        return isPrincipalServiceOid;
    }


    /**
     * Returns true or false depending upon if the OID is a service principal OID.
     *
     * @param dataPartitionId data partition id
     * @param oId             object id
     * @return boolean Whether it's a service principal account or not
     */
    private boolean isUserOidValid(final String dataPartitionId, final String oId) {
        boolean isValidUserOid = false;
        try {
            Validators.checkNotNullAndNotEmpty(oId, FIELD_NAME);
            UserItemRequestBuilder userItemRequestBuilder = graphServiceClientFactory.
                    getGraphServiceClient(dataPartitionId).
                    users().
                    byUserId(oId);

            String userName = userItemRequestBuilder.get().getGivenName();
            log.info(String.format("User OID %s validated, name: %s", oId, userName));
            isValidUserOid = true;
        } catch (Exception error) {
            log.info(String.format(EXCEPTION_WHILE, "Fetching the user oId: " + oId, error.getMessage()));
        }
        return isValidUserOid;
    }

    /**
     * Returns true or false depending upon if the Client ID is a service principal client ID.
     *
     * @param dataPartitionId data partition id
     * @param clientId        object id
     * @return boolean Whether it's a service principal account or not
     */
    private boolean isPrincipalServiceClientIdValid(final String dataPartitionId, final String clientId) {
        boolean isPrincipalServiceClientIdValid = false;
        try {
            Validators.checkNotNullAndNotEmpty(clientId, FIELD_NAME);
            ServicePrincipalCollectionResponse servicePrincipalCollectionResponse = graphServiceClientFactory.
                    getGraphServiceClient(dataPartitionId).
                    servicePrincipals().get(requestConfiguration ->
                        requestConfiguration.queryParameters.filter = "appId eq '" + clientId + "'"
                    );

            String dpName = servicePrincipalCollectionResponse.getValue().get(0).getDisplayName();
            log.info(String.format("Service principal %s validated", dpName));
            isPrincipalServiceClientIdValid = true;
        } catch (Exception ex) {
            log.info(String.format(EXCEPTION_WHILE, "Fetching the service principal by client ID: " + clientId, ex.getMessage()));
        }
        return isPrincipalServiceClientIdValid;
    }

    /***
     * Checks if an oID is an AAD Group OID.
     * @param dataPartitionId data-partition-id
     * @param oId object ID of group
     * @return true/false depending upon whether the graph API is returning a group of that oId
     */
    private boolean isAadGroupIdValid(final String dataPartitionId, final String oId) {
        boolean isAadGroupIdValid = false;
        try {
            Validators.checkNotNullAndNotEmpty(oId, FIELD_NAME);
            GroupItemRequestBuilder groupItemRequestBuilder = graphServiceClientFactory.
                    getGraphServiceClient(dataPartitionId).
                    groups().byGroupId(oId);

            String dpName = groupItemRequestBuilder.get().getDisplayName();
            log.info(String.format("Group OID %s validated", dpName));
            isAadGroupIdValid = true;
        } catch (Exception ex) {
            log.info(String.format(EXCEPTION_WHILE, "Fetching OID of group: " + oId, ex.getMessage()));
        }
        return isAadGroupIdValid;
    }

    /***
     * Checks if an OID is valid.
     * @param dataPartitionId data-partition
     * @param id OID to validate
     * @return true or false, depending upon if OID is valid
     */
    public boolean isOidValid(final String dataPartitionId, final String id) {
        boolean isOidValid = true;
        if (isPrincipalServiceOidValid(dataPartitionId, id)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "The given OID matches with a provisioned Service Principal. They should be added to OSDU groups via their Client ID. Please use the correct ID as the input");
        }

        //check if its client id of another service principal, otherwise check if it's a normal user
        if (!isPrincipalServiceClientIdValid(dataPartitionId, id)
                && !isUserOidValid(dataPartitionId, id)
                && !isAadGroupIdValid(dataPartitionId, id)) {
            isOidValid = false;
            log.error("OID: " + id + " Could not be validated as either a Service principal client id/user OId or a group OID");
        }
        return isOidValid;
    }
}