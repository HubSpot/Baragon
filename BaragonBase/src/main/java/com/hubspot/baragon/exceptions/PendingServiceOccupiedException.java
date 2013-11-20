package com.hubspot.baragon.exceptions;

import com.hubspot.baragon.models.ServiceInfo;

public class PendingServiceOccupiedException extends RuntimeException {
  private final ServiceInfo pendingService;

  public PendingServiceOccupiedException(ServiceInfo pendingService) {
    super(String.format("Pending service slot for %s is occupied by %s (contact: %s)", pendingService.getName(),
        pendingService.getId(), pendingService.getContactEmail()));
    this.pendingService = pendingService;
  }

  public ServiceInfo getPendingService() {
    return pendingService;
  }
}
