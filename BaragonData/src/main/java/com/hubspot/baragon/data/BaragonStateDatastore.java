package com.hubspot.baragon.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.utils.ZkParallelFetcher;

@Singleton
public class BaragonStateDatastore extends AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonStateDatastore.class);

  public static final String SERVICES_FORMAT = "/state";
  public static final String SERVICE_FORMAT = SERVICES_FORMAT + "/%s";
  public static final String UPSTREAM_FORMAT = SERVICE_FORMAT + "/%s";

  private final ZkParallelFetcher zkFetcher;

  @Inject
  public BaragonStateDatastore(CuratorFramework curatorFramework,
                               ObjectMapper objectMapper,
                               ZkParallelFetcher zkFetcher) {
    super(curatorFramework, objectMapper);
    this.zkFetcher = zkFetcher;
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
      writeToZk(String.format(UPSTREAM_FORMAT, serviceId, add), new UpstreamInfo(add, Optional.of(requestId)));
    }
  }

  public void updateStateNode() {
    try {
      writeToZk(SERVICES_FORMAT, computeAllServiceStates());
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<BaragonServiceState> getGlobalState() {
    return readFromZk(SERVICES_FORMAT, BARAGON_SERVICE_STATE_COLLECTION).or(Collections.<BaragonServiceState>emptyList());
  }

  private Collection<BaragonServiceState> computeAllServiceStates() throws Exception {
    Collection<String> services = new ArrayList<>();

    for (String service : getServices()) {
      services.add(ZKPaths.makePath(SERVICES_FORMAT, service));
    }

    final Map<String, BaragonService> serviceMap = zkFetcher.fetchDataInParallel(services, new BaragonServiceDeserializer());
    final Map<String, Collection<UpstreamInfo>> serviceToUpstreamInfoMap = fetchServiceToUpstreamInfoMap(services);

    final Collection<BaragonServiceState> serviceStates = new ArrayList<>(serviceMap.size());

    for (final Entry<String, BaragonService> serviceEntry : serviceMap.entrySet()) {
      BaragonService service = serviceEntry.getValue();
      Collection<UpstreamInfo> upstreams = serviceToUpstreamInfoMap.get(serviceEntry.getKey());

      serviceStates.add(new BaragonServiceState(service, Objects.firstNonNull(upstreams, Collections.<UpstreamInfo>emptyList())));
    }

    return serviceStates;
  }

  private Map<String, Collection<UpstreamInfo>> fetchServiceToUpstreamInfoMap(Collection<String> services) throws Exception {
    Map<String, String> upstreamToService = fetchUpstreamToServiceMap(services);

    Collection<String> upstreamPaths = new ArrayList<>(upstreamToService.size());
    for (Entry<String, String> entry : upstreamToService.entrySet()) {
      String service = entry.getValue();
      String servicePath = ZKPaths.makePath(SERVICES_FORMAT, service);

      String upstream = entry.getKey();
      String upstreamPath = ZKPaths.makePath(servicePath, upstream);

      upstreamPaths.add(upstreamPath);
    }

    Map<String, UpstreamInfo> upstreamToInfo = zkFetcher.fetchDataInParallel(upstreamPaths, new UpstreamInfoDeserializer());

    Map<String, Collection<UpstreamInfo>> serviceToUpstreamInfo = new HashMap<>(services.size());
    for (Entry<String, UpstreamInfo> entry : upstreamToInfo.entrySet()) {
      String upstream = entry.getKey();
      String service = upstreamToService.get(upstream);
      Preconditions.checkNotNull(service, String.format("Invalid state for upstream '%s'", upstream));

      if (serviceToUpstreamInfo.containsKey(service)) {
        serviceToUpstreamInfo.get(service).add(entry.getValue());
      } else {
        serviceToUpstreamInfo.put(service, Lists.newArrayList(entry.getValue()));
      }
    }

    return serviceToUpstreamInfo;
  }

  private Map<String, String> fetchUpstreamToServiceMap(Collection<String> services) throws Exception {
    Map<String, Collection<String>> serviceToUpstreams = zkFetcher.fetchChildrenInParallel(services);

    Map<String, String> upstreamToService = new HashMap<>();
    for (Entry<String, Collection<String>> entry : serviceToUpstreams.entrySet()) {
      String service = entry.getKey();

      for (String upstream : entry.getValue()) {
        upstreamToService.put(upstream, service);
      }
    }

    return upstreamToService;
  }

  private class BaragonServiceDeserializer implements Function<byte[], BaragonService> {
    @Override
    public BaragonService apply(byte[] data) {
      return deserialize(data, BaragonService.class);
    }
  }

  private class UpstreamInfoDeserializer implements Function<byte[], UpstreamInfo> {
    @Override
    public UpstreamInfo apply(byte[] input) {
      return deserialize(input, UpstreamInfo.class);
    }
  }

  private static final TypeReference<Collection<BaragonServiceState>> BARAGON_SERVICE_STATE_COLLECTION = new TypeReference<Collection<BaragonServiceState>>() {};
}
