package com.hubspot.baragon.managers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonAliasDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonGroupAlias;
import com.hubspot.baragon.models.BaragonRequest;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliasManager {
  private static final Logger LOG = LoggerFactory.getLogger(AliasManager.class);

  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonAliasDatastore aliasDatastore;

  @Inject
  public AliasManager(
    BaragonLoadBalancerDatastore loadBalancerDatastore,
    BaragonAliasDatastore aliasDatastore
  ) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.aliasDatastore = aliasDatastore;
  }

  public BaragonRequest updateForAliases(BaragonRequest original) {
    Set<String> edgeCacheDomains = new HashSet<>(
      original.getLoadBalancerService().getEdgeCacheDomains()
    );
    if (original.getLoadBalancerService().getEdgeCacheDNS().isPresent()) {
      edgeCacheDomains.add(original.getLoadBalancerService().getEdgeCacheDNS().get());
    }
    return original.withUpdatedGroups(
      processAliases(
        original.getLoadBalancerService().getLoadBalancerGroups(),
        original.getLoadBalancerService().getDomains(),
        edgeCacheDomains
      )
    );
  }

  public BaragonGroupAlias processAliases(
    Set<String> groups,
    Set<String> domains,
    Set<String> edgeCacheDomains
  ) {
    Set<String> allGroups = new HashSet<>();
    Set<String> allDomains = new HashSet<>(domains);
    Set<String> allEdgeCacheDomains = new HashSet<>(edgeCacheDomains);
    for (String group : groups) {
      Optional<BaragonGroupAlias> maybeAlias = aliasDatastore.getAlias(group);
      if (maybeAlias.isPresent()) {
        allGroups.addAll(maybeAlias.get().getGroups());
        allDomains.addAll(maybeAlias.get().getDomains());
        allEdgeCacheDomains.addAll(maybeAlias.get().getEdgeCacheDomains());
      } else {
        Optional<BaragonGroup> maybeGroup = loadBalancerDatastore.getLoadBalancerGroup(
          group
        );
        // If we don't add the default domain here, it can get missed in the normal default domains
        // call, which excludes it based on the presence of any other domain for the group
        if (maybeGroup.isPresent() && maybeGroup.get().getDefaultDomain().isPresent()) {
          allDomains.add(maybeGroup.get().getDefaultDomain().get());
        }
        allGroups.add(group);
      }
    }
    return new BaragonGroupAlias(allGroups, allDomains, allEdgeCacheDomains);
  }
}
