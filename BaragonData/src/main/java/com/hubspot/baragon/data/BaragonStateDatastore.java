package com.hubspot.baragon.data;

import java.util.Collection;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;

@Singleton
public class BaragonStateDatastore extends AbstractDataStore {
  public static final String SERVICES_FORMAT = "/state";
  public static final String SERVICE_FORMAT = SERVICES_FORMAT + "/%s";
  public static final String UPSTREAM_FORMAT = SERVICE_FORMAT + "/%s";

  @Inject
  public BaragonStateDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public Collection<String> getServices() {
    return getChildren(SERVICES_FORMAT);
  }

  public void addService(BaragonService service) {
    writeToZk(String.format(SERVICE_FORMAT, service.getServiceId()), service);
  }

  public Optional<BaragonService> getService(String serviceId) {
    return readFromZk(String.format(SERVICE_FORMAT, serviceId), BaragonService.class);
  }

  public void removeService(String serviceId) {
    for (String upstream : getUpstreams(serviceId)) {
      deleteNode(String.format(UPSTREAM_FORMAT, serviceId, upstream));
    }

    deleteNode(String.format(SERVICE_FORMAT, serviceId));
  }

  public Collection<String> getUpstreams(String serviceId) {
    return getChildren(String.format(SERVICE_FORMAT, serviceId));
  }

  public Map<String, UpstreamInfo> getUpstreamsMap(String serviceId) {
    final Collection<String> upstreams = getUpstreams(serviceId);

    final Map<String, UpstreamInfo> upstreamsMap = Maps.newHashMap();

    for (String upstream : upstreams) {
      final Optional<UpstreamInfo> maybeUpstreamInfo = getUpstream(serviceId, upstream);
      if (maybeUpstreamInfo.isPresent()) {
        upstreamsMap.put(upstream, maybeUpstreamInfo.get());
      }
    }

    return upstreamsMap;
  }

  public Optional<UpstreamInfo> getUpstream(String serviceId, String upstream) {
    return readFromZk(String.format(UPSTREAM_FORMAT, serviceId, upstream), UpstreamInfo.class);
  }

  public void removeUpstreams(String serviceId, Collection<String> upstreams) {
    for (String remove : upstreams) {
      deleteNode(String.format(UPSTREAM_FORMAT, serviceId, remove));
    }
  }

  public void addUpstreams(String requestId, String serviceId, Collection<String> upstreams) {
    for (String add : upstreams) {
      writeToZk(String.format(UPSTREAM_FORMAT, serviceId, add), new UpstreamInfo(add, requestId));
    }
  }
}
