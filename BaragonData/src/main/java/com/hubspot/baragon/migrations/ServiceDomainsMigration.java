package com.hubspot.baragon.migrations;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonService;

public class ServiceDomainsMigration extends ZkDataMigration {

  private final BaragonStateDatastore baragonStateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public ServiceDomainsMigration(BaragonStateDatastore baragonStateDatastore,
                            BaragonLoadBalancerDatastore loadBalancerDatastore) {
    super(2);
    this.baragonStateDatastore = baragonStateDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @Override
  public void applyMigration() {
    try {
      for (String serviceId : baragonStateDatastore.getServices()) {
        Optional<BaragonService> service = baragonStateDatastore.getService(serviceId);
        if (service.isPresent()) {
          Set<String> updatedDomainsWithDefaults = loadBalancerDatastore.getDomainsWithDefaults(service.get());
          baragonStateDatastore.saveService(service.get().withDomains(updatedDomainsWithDefaults));
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
