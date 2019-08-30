package com.hubspot.baragon.kubernetes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonAliasDatastore;
import com.hubspot.baragon.models.BaragonGroupAlias;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;

@Singleton
public class KubernetesWatcher implements Watcher<Endpoints> {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesWatcher.class);

  private final KubernetesEndpointListener listener;
  private final KubernetesConfiguration kubernetesConfiguration;
  private final BaragonAliasDatastore aliasDatastore;
  private final KubernetesClient kubernetesClient;

  @Inject
  public KubernetesWatcher(KubernetesEndpointListener listener,
                           KubernetesConfiguration kubernetesConfiguration,
                           BaragonAliasDatastore aliasDatastore,
                           @Named(KubernetesWatcherModule.BARAGON_KUBERNETES_CLIENT) KubernetesClient kubernetesClient) {
    this.listener = listener;
    this.kubernetesConfiguration = kubernetesConfiguration;
    this.aliasDatastore = aliasDatastore;
    this.kubernetesClient = kubernetesClient;
  }

  public Watch createWatch() {
    // TODO - better filters?
    return kubernetesClient.endpoints()
        .inAnyNamespace()
        .withLabels(kubernetesConfiguration.getBaragonLabelFilter())
        .withLabel(kubernetesConfiguration.getServiceNameLabel())
        .watch(this);
  }

  @Override
  public void eventReceived(Action action, Endpoints endpoints) {
    Map<String, String> labels = endpoints.getMetadata().getLabels();
    String serviceName = labels.get(kubernetesConfiguration.getServiceNameLabel());
    if (serviceName == null) {
      LOG.warn("Could not get service name for endpoint update (action: {}, endpoints: {})", action, endpoints);
      return;
    }

    Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
    String basePath = annotations.get(kubernetesConfiguration.getBasePathAnnotation());
    if (basePath == null) {
      LOG.warn("Could not get base path for endpoint update (action: {}, endpoints: {})", action, endpoints);
      return;
    }

    String upstreamGroup = annotations.getOrDefault(kubernetesConfiguration.getUpstreamGroupsLabel(), "default");
    if (!kubernetesConfiguration.getUpstreamGroups().contains(upstreamGroup)) {
      LOG.warn("Upstream group not managed by baragon, skipping (action: {}, endpoints: {})", action, endpoints);
      return;
    }

    String lbGroupsString = annotations.get(kubernetesConfiguration.getLbGroupsAnnotation());
    if (lbGroupsString == null) {
      LOG.warn("Could not get load balancer groups for kubernetes event (action: {}, endpoints: {})", action, endpoints);
      return;
    }
    Optional<String> domainsString = Optional.fromNullable(annotations.get(kubernetesConfiguration.getDomainsAnnotation()));
    // non-aliased edgeCacheDomains not yet supported
    BaragonGroupAlias groupsAndDomains = aliasDatastore.processAliases(
        new HashSet<>(Arrays.asList(lbGroupsString.split(","))),
        domainsString.transform((d) -> new HashSet<>(Arrays.asList(d.split(",")))).or(new HashSet<>()),
        Collections.emptySet()
    );

    Optional<String> ownerString = Optional.fromNullable(annotations.get(kubernetesConfiguration.getOwnersAnnotation()));
    List<String> owners = ownerString.transform((o) -> Arrays.asList(o.split(","))).or(new ArrayList<>());

    Optional<String> templateName = Optional.fromNullable(annotations.get(kubernetesConfiguration.getTemplateNameAnnotation()));

    BaragonService service = new BaragonService(
        serviceName,
        owners,
        basePath,
        Collections.emptyList(), // additionalPaths not yet supported
        groupsAndDomains.getGroups(),
        Collections.emptyMap(), // custom options not yet supported
        templateName,
        groupsAndDomains.getDomains(),
        Optional.absent(),
        groupsAndDomains.getEdgeCacheDomains(),
        false // Always using IPs to start with
    );
    listener.processUpdate(service, parseActiveUpstreams(endpoints, upstreamGroup));
  }

  private List<UpstreamInfo> parseActiveUpstreams(Endpoints endpoints, String upstreamGroup) {
    List<UpstreamInfo> upstreams = new ArrayList<>();
    for (EndpointSubset subset : endpoints.getSubsets()) {
      // TODO - flag for http vs https?
      // TODO - pick correct port?
      int port = subset.getPorts().get(0).getPort();
      for (EndpointAddress address : subset.getAddresses()) {
        // TODO - rack +
        upstreams.add(new UpstreamInfo(
            String.format("%s:%d", address.getIp(), port),
            Optional.absent(),
            Optional.absent(),
            Optional.of(upstreamGroup)
        ));
      }
    }
    return upstreams;
  }

  @Override
  public void onClose(KubernetesClientException cause) {

  }
}
