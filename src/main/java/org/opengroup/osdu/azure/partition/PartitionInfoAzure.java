package org.opengroup.osdu.azure.partition;

import com.azure.security.keyvault.secrets.SecretClient;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.partition.Property;

/**
 * Azure data partition variables.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartitionInfoAzure {

    @SerializedName("id")
    private Property idConfig;

    @SerializedName("name")
    private Property nameConfig;

    @SerializedName("compliance-ruleset")
    private Property complianceRulesetConfig;

    @SerializedName("elastic-endpoint")
    private Property elasticEndpointConfig;

    @SerializedName("elastic-username")
    private Property elasticUsernameConfig;

    @SerializedName("elastic-password")
    private Property elasticPasswordConfig;

    @Builder.Default
    @SerializedName("elastic-ssl-enabled")
    private Property elasticSslEnabledConfig = Property.builder().value(true).build();

    @SerializedName("cosmos-connection")
    private Property cosmosConnectionConfig;

    @SerializedName("cosmos-endpoint")
    private Property cosmosEndpointConfig;

    @SerializedName("cosmos-primary-key")
    private Property cosmosPrimaryKeyConfig;

    @SerializedName("sb-connection")
    private Property sbConnectionConfig;

    @SerializedName("storage-account-key")
    private Property storageAccountKeyConfig;

    @SerializedName("storage-account-name")
    private Property storageAccountNameConfig;

    @SerializedName("storage-account-blob-endpoint")
    private Property storageAccountBlobEndpointConfig;

    @SerializedName("hierarchical-storage-account-key")
    private Property hierarchicalStorageAccountKeyConfig;

    @SerializedName("hierarchical-storage-account-name")
    private Property hierarchicalStorageAccountNameConfig;

    @SerializedName("hierarchical-storage-blob-endpoint")
    private Property hierarchicalStorageAccountBlobEndpointConfig;

    @SerializedName("sb-namespace")
    private Property sbNamespaceConfig;

    @SerializedName("eventgrid-recordstopic")
    private Property eventGridRecordsTopicEndpointConfig;

    @SerializedName("eventgrid-recordstopic-accesskey")
    private Property eventGridRecordsTopicAccessKeyConfig;

    @SerializedName("eventgrid-resourcegroup")
    private Property eventGridResourceGroupConfig;

    @SerializedName("encryption-key-identifier")
    private Property cryptographyEncryptionKeyIdentifierConfig;

    @SerializedName("policy-service-enabled")
    private Property policyServiceConfig = Property.builder().sensitive(false).build();

    @Builder.Default
    @SerializedName("airflow-enabled")
    private Property airflowEnabledConfig = Property.builder().value("false").sensitive(false).build();

    @SerializedName("airflow-endpoint")
    private Property airflowEndpointConfig;

    @SerializedName("airflow-username")
    private Property airflowUsernameConfig;

    @SerializedName("airflow-password")
    private Property airflowPasswordConfig;

    @SerializedName("ingest-storage-account-key")
    private Property ingestStorageAccountKeyConfig;

    @SerializedName("ingest-storage-account-name")
    private Property ingestStorageAccountNameConfig;


    private Property azureSubscriptionIdConfig = Property.builder().value("subscription-id").sensitive(true).build();

    private Property servicePrincipalAppIdConfig = Property.builder().value("app-dev-sp-username").sensitive(true).build();

    private SecretClient secretClient;

    /**
     * @return partition id
     */
    public String getId() {
        return String.valueOf(this.getIdConfig().getValue());
    }

    /**
     * @return policy service config
     */
    public String getPolicySvcConfig() {
        return String.valueOf(this.getPolicyServiceConfig().getValue());
    }

    /**
     * @return partition name
     */
    public String getName() {
        return String.valueOf(this.getNameConfig().getValue());
    }

    /**
     * @return partition compliance ruleset
     */
    public String getComplianceRuleset() {
        return String.valueOf(this.getComplianceRulesetConfig().getValue());
    }

    /**
     * @return partition service principle id
     */
    public String getServicePrincipalAppId() {
        if (this.getServicePrincipalAppIdConfig().isSensitive()) {
            return getSecret(this.getServicePrincipalAppIdConfig());
        }
        return String.valueOf(this.getServicePrincipalAppIdConfig().getValue());
    }

    /**
     * @return partition elastic endpoint
     */
    public String getElasticEndpoint() {
        if (this.getElasticEndpointConfig().isSensitive()) {
            return getSecret(this.getElasticEndpointConfig());
        }
        return String.valueOf(this.getElasticEndpointConfig().getValue());
    }

    /**
     * @return partition elastic username
     */
    public String getElasticUsername() {
        if (this.getElasticUsernameConfig().isSensitive()) {
            return getSecret(this.getElasticUsernameConfig());
        }
        return String.valueOf(this.getElasticUsernameConfig().getValue());
    }

    /**
     * @return partition elastic password
     */
    public String getElasticPassword() {
        if (this.getElasticPasswordConfig().isSensitive()) {
            return getSecret(this.getElasticPasswordConfig());
        }
        return String.valueOf(this.getElasticPasswordConfig().getValue());
    }

    /**
     * @return partition airflow endpoint
     */
    public Boolean getAirflowEnabled() {
        return Boolean.parseBoolean((String) this.getAirflowEnabledConfig().getValue());
    }

    /**
     * @return partition airflow endpoint
     */
    public String getAirflowEndpoint() {
        if (this.getAirflowEndpointConfig().isSensitive()) {
            return getSecret(this.getAirflowEndpointConfig());
        }
        return String.valueOf(this.getAirflowEndpointConfig().getValue());
    }

    /**
     * @return partition airflow username
     */
    public String getAirflowUsername() {
        if (this.getAirflowUsernameConfig().isSensitive()) {
            return getSecret(this.getAirflowUsernameConfig());
        }
        return String.valueOf(this.getAirflowUsernameConfig().getValue());
    }

    /**
     * @return partition airflow password
     */
    public String getAirflowPassword() {
        if (this.getAirflowPasswordConfig().isSensitive()) {
            return getSecret(this.getAirflowPasswordConfig());
        }
        return String.valueOf(this.getAirflowPasswordConfig().getValue());
    }

    /**
     * @return partition elastic ssl enabled
     */
    public String getElasticSslEnabled() {
        if (this.getElasticSslEnabledConfig().isSensitive()) {
            return getSecret(this.getElasticSslEnabledConfig());
        }
        return String.valueOf(this.getElasticSslEnabledConfig().getValue());
    }

    /**
     * @return partition cosmosdb connection
     */
    public String getCosmosConnection() {
        if (this.getCosmosConnectionConfig().isSensitive()) {
            return getSecret(this.getCosmosConnectionConfig());
        }
        return String.valueOf(this.getCosmosConnectionConfig().getValue());
    }

    /**
     * @return partition cosmosdb endpoint
     */
    public String getCosmosEndpoint() {
        if (this.getCosmosEndpointConfig().isSensitive()) {
            return getSecret(this.getCosmosEndpointConfig());
        }
        return String.valueOf(this.getCosmosEndpointConfig().getValue());
    }

    /**
     * @return partition cosmosdb primary-key
     */
    public String getCosmosPrimaryKey() {
        if (this.getCosmosPrimaryKeyConfig().isSensitive()) {
            return getSecret(this.getCosmosPrimaryKeyConfig());
        }
        return String.valueOf(this.getCosmosPrimaryKeyConfig().getValue());
    }

    /**
     * @return partition service bus connection string
     */
    public String getSbConnection() {
        if (this.getSbConnectionConfig().isSensitive()) {
            return getSecret(this.getSbConnectionConfig());
        }
        return String.valueOf(this.getSbConnectionConfig().getValue());
    }

    /**
     * @return partition service bus namespace
     */
    public String getSbNamespace() {
        if (this.getSbNamespaceConfig().isSensitive()) {
            return getSecret(this.getSbNamespaceConfig());
        }
        return String.valueOf(this.getSbNamespaceConfig().getValue());
    }

    /**
     * @return partition storage account key
     */
    public String getStorageAccountKey() {
        if (this.getStorageAccountKeyConfig().isSensitive()) {
            return getSecret(this.getStorageAccountKeyConfig());
        }
        return String.valueOf(this.getStorageAccountKeyConfig().getValue());
    }

    /**
     * @return partition storage account name
     */
    public String getStorageAccountName() {
        if (this.getStorageAccountNameConfig().isSensitive()) {
            return getSecret(this.getStorageAccountNameConfig());
        }
        return String.valueOf(this.getStorageAccountNameConfig().getValue());
    }

    /**
     * @return Storage blob endpoint.
     */
    public String getStorageBlobEndpoint() {
        // if partition info does not have blob endpoint config, return existing logic OR
        if (this.getStorageAccountBlobEndpointConfig() == null) {
            CoreLoggerFactory.getInstance().getLogger(PartitionInfoAzure.class).info("No Blob Endpoint Config. Returning legacy storage endpoint");
            return createStorageEndpointFromStorageAccountName();
        }

        if (this.getStorageAccountBlobEndpointConfig().isSensitive()) {
            String storageEndpoint = getSecretWithDefault(this.getStorageAccountBlobEndpointConfig(), null);
            // Service is upgraded without infra upgrades.
            // if partition info has blob endpoint but secret does not exist return existing logic.
            if (storageEndpoint == null) {
                return createStorageEndpointFromStorageAccountName();
            }

            // Blob endpoint is available in KeyVault. Return it.
            return getSecret(this.getStorageAccountBlobEndpointConfig());
        }

        return String.valueOf(this.getStorageAccountBlobEndpointConfig().getValue());
    }

    /**
     *
     * @return Hierarchical blob endpoint.
     */
    public String getHierarchicalStorageAccountBlobEndpoint() {
        // if partition info does not have hierarchical endpoint config, return existing logic OR
        if (this.getHierarchicalStorageAccountBlobEndpointConfig() == null) {
            CoreLoggerFactory.getInstance().getLogger(PartitionInfoAzure.class).info("No Hierarchical Endpoint Config. Returning legacy datalake endpoint");
            return createStorageEndpointFromHierarchicalStorageAccountName();
        }

        if (this.getHierarchicalStorageAccountBlobEndpointConfig().isSensitive()) {
            String hierarchicalBlobEndpoint = getSecretWithDefault(this.getHierarchicalStorageAccountBlobEndpointConfig(), null);
            // Service is upgraded without infra upgrades.
            // if partition info has blob endpoint but secret does not exist return existing logic.
            if (hierarchicalBlobEndpoint == null) {
                return createStorageEndpointFromHierarchicalStorageAccountName();
            }
            // Blob endpoint is available in KeyVault. Return it.
            return getSecret(this.getHierarchicalStorageAccountBlobEndpointConfig());
        }

        return String.valueOf(this.getHierarchicalStorageAccountBlobEndpointConfig().getValue());
    }

    /**
     * @return file collection feature storage account key
     */
    public String getHierarchicalStorageAccountKey() {
        if (this.getHierarchicalStorageAccountKeyConfig().isSensitive()) {
            return getSecret(this.getHierarchicalStorageAccountKeyConfig());
        }
        return String.valueOf(this.getHierarchicalStorageAccountKeyConfig().getValue());
    }

    /**
     * @return file collection feature storage account name
     */
    public String getHierarchicalStorageAccountName() {
        if (this.getHierarchicalStorageAccountNameConfig().isSensitive()) {
            return getSecret(this.getHierarchicalStorageAccountNameConfig());
        }
        return String.valueOf(this.getHierarchicalStorageAccountNameConfig().getValue());
    }

    /**
     * @return partition event grid topic endpoint
     */
    public String getEventGridRecordsTopicEndpoint() {
        if (this.getEventGridRecordsTopicEndpointConfig().isSensitive()) {
            return getSecret(this.getEventGridRecordsTopicEndpointConfig());
        }
        return String.valueOf(this.getEventGridRecordsTopicEndpointConfig().getValue());
    }

    /**
     * @return partition event grid topic key
     */
    public String getEventGridRecordsTopicAccessKey() {
        if (this.getEventGridRecordsTopicAccessKeyConfig().isSensitive()) {
            return getSecret(this.getEventGridRecordsTopicAccessKeyConfig());
        }
        return String.valueOf(this.getEventGridRecordsTopicAccessKeyConfig().getValue());
    }

    /**
     * @return partition event grid ResourceGroup
     */
    public String getEventGridResourceGroup() {
        if (this.getEventGridResourceGroupConfig().isSensitive()) {
            return getSecret(this.getEventGridResourceGroupConfig());
        }
        return String.valueOf(this.getEventGridResourceGroupConfig().getValue());
    }

    /**
     * @return partition Encryption Key Identifier
     */
    public String getCryptographyEncryptionKeyIdentifier() {
        if (this.getCryptographyEncryptionKeyIdentifierConfig().isSensitive()) {
            return getSecret(this.getCryptographyEncryptionKeyIdentifierConfig());
        }
        return String.valueOf(this.getCryptographyEncryptionKeyIdentifierConfig().getValue());
    }

    /**
     * @return partition azure subscriptionId
     */
    public String getAzureSubscriptionId() {
        if (this.getAzureSubscriptionIdConfig().isSensitive()) {
            return getSecret(this.getAzureSubscriptionIdConfig());
        }
        return String.valueOf(this.getAzureSubscriptionIdConfig().getValue());
    }

    /**
     * @param client KV secret client
     */
    public void configureSecretClient(final SecretClient client) {
        this.secretClient = client;
    }

    /**
     * @param p partition property
     * @return secret value
     */
    private String getSecret(final Property p) {
        return KeyVaultFacade.getSecretWithValidation(this.secretClient, String.valueOf(p.getValue()));
    }

    /**
     *
     * Fetch Secret from KeyVault. If does not exist, return defaultValue
     * @param p Property
     * @param defaultValue Default value to return
     * @return Secret Value
     */
    private String getSecretWithDefault(final Property p, final String defaultValue) {
        return KeyVaultFacade.getSecretWithDefault(this.secretClient, String.valueOf(p.getValue()), defaultValue);
    }

    /**
     * Generate the Storage Endpoint from the StorageAccount Name.
     * @return the storage endpoint.
     */
    private String createStorageEndpointFromStorageAccountName() {
        return String.format("https://%s.blob.core.windows.net", getStorageAccountName());
    }

    /**
     * Generate Hierarchical Storage endpoint from Storage Account name.
     * @return the storage endpoint.
     */
    private String createStorageEndpointFromHierarchicalStorageAccountName() {
        return String.format("https://%s.dfs.core.windows.net", getHierarchicalStorageAccountName());
    }

    /**
     * @return ingestion storage account key
     */
    public String getIngestStorageAccountKey() {
        if (this.getIngestStorageAccountKeyConfig().isSensitive()) {
            return getSecret(this.getIngestStorageAccountKeyConfig());
        }
        return String.valueOf(this.getIngestStorageAccountKeyConfig().getValue());
    }

    /**
     * @return ingestion storage account name
     */
    public String getIngestStorageAccountName() {
        if (this.getIngestStorageAccountNameConfig().isSensitive()) {
            return getSecret(this.getIngestStorageAccountNameConfig());
        }
        return String.valueOf(this.getIngestStorageAccountNameConfig().getValue());
    }
}