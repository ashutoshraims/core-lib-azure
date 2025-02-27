package org.opengroup.osdu.azure.partition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.PartitionServiceConfiguration;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class PartitionServiceFactoryTest {

    @Mock
    private PartitionServiceConfiguration partitionServiceConfiguration;
    @InjectMocks
    private PartitionServiceFactory sut;

    @Test
    public void should_return_validFactory() {
        IPartitionFactory factory = this.sut.partitionFactory();
        assertNotNull(factory);
    }
}
