package org.opengroup.osdu.azure.multitenancy;

import com.azure.security.keyvault.secrets.SecretClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.partition.Property;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantFactoryImplTest {

    private static final String tenantName = "opendes";
    private static final String notFound = "not-found";
    private static final String serviceprincipalAppId = "sp-id";
    private static final String complianceRuleSet = "compliance-rule-set";

    @InjectMocks
    private TenantFactoryImpl tenantFactory;
    @Mock
    private PartitionServiceClient partitionServiceClient;
    @Mock
    private ILogger logger;

    @BeforeEach
    public void init() {
        PartitionInfoAzure expectedPartitionInfo = PartitionInfoAzure.builder()
                .idConfig(Property.builder().value(tenantName).build())
                .servicePrincipalAppIdConfig(Property.builder().value(serviceprincipalAppId).build())
                .complianceRulesetConfig(Property.builder().value(complianceRuleSet).build())
                .build();
        lenient().when(partitionServiceClient.getPartition(tenantName)).thenReturn(expectedPartitionInfo);

        AppException notFoundException = new AppException(HttpStatus.SC_NOT_FOUND, "Not found", String.format("Error getting tenant: %s via partition service.", tenantName + notFound));
        lenient().when(partitionServiceClient.getPartition(tenantName + notFound)).thenThrow(notFoundException);

        initAppDevSpServicePrincipal();
    }

    @Test
    public void returnsTrueWhenTenantNameFound() {
        boolean result = tenantFactory.exists(tenantName);

        assertTrue(result);
    }

    @Test
    public void returnsFalseWhenTenantNameNotFound() {
        boolean result = tenantFactory.exists(tenantName + notFound);

        assertFalse(result);
    }

    @Test
    public void returnsTenantInfoObjectWhenTenantNameFound() {
        TenantInfo result = tenantFactory.getTenantInfo(tenantName);

        TenantInfo expected = new TenantInfo();
        expected.setName(tenantName);
        expected.setDataPartitionId(tenantName);
        expected.setComplianceRuleSet(complianceRuleSet);
        expected.setServiceAccount(serviceprincipalAppId);

        assertEquals(expected, result);
    }

    @Test
    public void returnsNullWhenTenantNameNotFound() {
        TenantInfo result = tenantFactory.getTenantInfo(tenantName + notFound);

        assertNull(result);
    }

    @Test
    public void returnsEmptyListTenants() {
        List<TenantInfo> result = new ArrayList<>(tenantFactory.listTenantInfo());
        assertNotNull(result);
    }

    @Test
    public void returnsListOfTenants() {
        List<String> partitions = new ArrayList<>();
        partitions.add("tenant1");
        partitions.add("tenant2");
        when(partitionServiceClient.listPartitions()).thenReturn(partitions);

        List<TenantInfo> result = new ArrayList<>(tenantFactory.listTenantInfo());
        assertNotNull(result);
        assertEquals(partitions.size(), result.size());
    }

    private void initAppDevSpServicePrincipal() {
        Field appDevSpUsernameField = ReflectionUtils.findField(TenantFactoryImpl.class, "appDevSpUsername");
        appDevSpUsernameField.setAccessible(true);
        ReflectionUtils.setField(appDevSpUsernameField, tenantFactory, serviceprincipalAppId);
    }
}
