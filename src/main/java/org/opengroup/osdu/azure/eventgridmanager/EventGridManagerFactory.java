package org.opengroup.osdu.azure.eventgridmanager;

import com.microsoft.azure.management.eventgrid.v2020_04_01_preview.implementation.EventGridManager;

/**
 * Interface for Event Grid Manager Factory to return appropriate EventGridManager based on the data partition id.
 */
public interface EventGridManagerFactory {

  /**
   * @param partitionId partition id
   * @return EventGridManager
   */
  EventGridManager getManager(String partitionId);
}
