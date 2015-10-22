package com.hubspot.baragon.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.utils.ZkParallelFetcher;

@Singleton
public class BaragonStateDatastore extends AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonStateDatastore.class);

  public static final String SERVICES_FORMAT = "/state";
  public static final String LAST_UPDATED_FORMAT = "/state-last-updated";
  public static final String SERVICE_FORMAT = SERVICES_FORMAT + "/%s";
  public static final String UPSTREAM_FORMAT = SERVICE_FORMAT + "/%s";

  private final ZkParallelFetcher zkFetcher;

  @Inject
  public BaragonStateDatastore(CuratorFramework curatorFramework,
                               ObjectMapper objectMapper,
                               ZkParallelFetcher zkFetcher,
                               ZooKeeperConfiguration zooKeeperConfiguration) {
    super(curatorFramework, objectMapper, zooKeeperConfiguration);
    this.zkFetcher = zkFetcher;
  }

  @Timed
  public Collection<String> getServices() {
    return getChildren(SERVICES_FORMAT);
  }

  @Timed
  public void addService(BaragonService service) {
    writeToZk(String.format(SERVICE_FORMAT, service.getServiceId()), service);
  }

  @Timed
  public boolean serviceExists(String serviceId) {
    return nodeExists(String.format(SERVICE_FORMAT, serviceId));
  }

  @Timed
  public Optional<BaragonService> getService(String serviceId) {
    return readFromZk(String.format(SERVICE_FORMAT, serviceId), BaragonService.class);
  }

  @Timed
  public void removeService(String serviceId) {
    for (String upstream : getUpstreamNodes(serviceId)) {
      deleteNode(String.format(UPSTREAM_FORMAT, serviceId, upstream));
    }

    deleteNode(String.format(SERVICE_FORMAT, serviceId));
  }

  @Timed
  private Collection<String> getUpstreamNodes(String serviceId) {
    return getChildren(String.format(SERVICE_FORMAT, serviceId));
  }

  @Timed
  public Map<String, UpstreamInfo> getUpstreamsMap(String serviceId) throws Exception {
    final Collection<String> upstreamNodes = getUpstreamNodes(serviceId);
    final Collection<String> upstreamPaths = new ArrayList<>(upstreamNodes.size());
    for (String upstreamNode : upstreamNodes) {
      upstreamPaths.add(String.format(UPSTREAM_FORMAT, serviceId, upstreamNode));
    }

    return Maps.uniqueIndex(zkFetcher.fetchDataInParallel(upstreamPaths, new BaragonDeserializer<>(objectMapper, UpstreamInfo.class)).values(), new UpstreamKeyFunction());
  }

  @Timed
  public void removeUpstreams(String serviceId, Collection<UpstreamInfo> upstreams) {
    for (UpstreamInfo upstreamInfo : upstreams) {
      deleteNode(String.format(UPSTREAM_FORMAT, serviceId, sanitizeNodeName(upstreamInfo.getUpstream())));
    }
  }

  @Timed
  public void addUpstreams(String serviceId, Collection<UpstreamInfo> upstreams) {
    for (UpstreamInfo upstreamInfo : upstreams) {
      writeToZk(String.format(UPSTREAM_FORMAT, serviceId, sanitizeNodeName(upstreamInfo.getUpstream())), upstreamInfo);
    }
  }

  @Timed
  public void setUpstreams(String serviceId, Collection<UpstreamInfo> upstreams) throws Exception {
    for (UpstreamInfo upstreamInfo : getUpstreamsMap(serviceId).values()) {
      deleteNode(String.format(UPSTREAM_FORMAT, serviceId, sanitizeNodeName(upstreamInfo.getUpstream())));
    }
    for (UpstreamInfo upstreamInfo : upstreams) {
      writeToZk(String.format(UPSTREAM_FORMAT, serviceId, sanitizeNodeName(upstreamInfo.getUpstream())), upstreamInfo);
    }
  }

  @Timed
  public void updateStateNode() {
    try {
      LOG.info("Starting state node update");
      writeToZk(SERVICES_FORMAT, computeAllServiceStates());
      LOG.info("Finished state node update");
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Timed
  public Collection<BaragonServiceState> getGlobalState() {
    try {
      LOG.info("Starting to compute all service states");
      return computeAllServiceStates();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    } finally {
      LOG.info("Finished computing all service states");
    }
  }

  public byte[] getGlobalStateAsBytes() {
    return serialize(getGlobalState());
  }

  @Timed
  public int getGlobalStateSize() {
    try {
      final Stat stat = curatorFramework.checkExists().forPath(SERVICES_FORMAT);

      if (stat != null) {
        return stat.getDataLength();
      } else {
        return 0;
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void incrementStateVersion() {
    writeToZk(LAST_UPDATED_FORMAT, System.currentTimeMillis());
  }

  public Optional<Integer> getStateVersion() {
    try {
      final Stat stat = curatorFramework.checkExists().forPath(LAST_UPDATED_FORMAT);

      if (stat != null) {
        return Optional.of(stat.getVersion());
      } else {
        return Optional.absent();
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private Collection<BaragonServiceState> computeAllServiceStates() throws Exception {
    Collection<String> services = new ArrayList<>();

    for (String service : getServices()) {
      services.add(ZKPaths.makePath(SERVICES_FORMAT, service));
    }

    final Map<String, BaragonService> serviceMap = zkFetcher.fetchDataInParallel(services, new BaragonDeserializer<>(objectMapper, BaragonService.class));
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
    Map<String, Collection<String>> serviceToUpstreams = zkFetcher.fetchChildrenInParallel(services);
    Map<String, UpstreamInfo> upstreamToInfo = zkFetcher.fetchDataInParallel(upstreamPaths(serviceToUpstreams), new BaragonDeserializer<>(objectMapper, UpstreamInfo.class));

    Map<String, Collection<UpstreamInfo>> serviceToUpstreamInfo = new HashMap<>(services.size());

    // Services modified while we fetched upstream information
    Set<String> modifiedServices = new HashSet<>();
    for (Entry<String, Collection<String>> entry : serviceToUpstreams.entrySet()) {
      String service = entry.getKey();

      for (String upstream : entry.getValue()) {
        if (!upstreamToInfo.containsKey(upstream)) {
          modifiedServices.add(service);
        }
      }

      if (modifiedServices.contains(service)) {
        continue;
      }

      for (String upstream : entry.getValue()) {
        if (!serviceToUpstreamInfo.containsKey(entry.getKey())) {
          serviceToUpstreamInfo.put(entry.getKey(), Lists.newArrayList(upstreamToInfo.get(upstream)));
        } else {
          serviceToUpstreamInfo.get(entry.getKey()).add(upstreamToInfo.get(upstream));
        }
      }
    }

    // Fetch upstream info for the modified services
    serviceToUpstreamInfo.putAll(fetchServiceToUpstreamInfoMap(modifiedServices));

    return serviceToUpstreamInfo;
  }

  private Collection<String> upstreamPaths(Map<String, Collection<String>> serviceToUpstreams) {
    Collection<String> allUpstreamPaths = new ArrayList<>();
    for (Entry<String, Collection<String>> entry : serviceToUpstreams.entrySet()) {
      for (String upstream : entry.getValue()) {
        allUpstreamPaths.add(String.format(UPSTREAM_FORMAT, entry.getKey(), upstream));
      }
    }
    return allUpstreamPaths;
  }

  public static class BaragonDeserializer<T> implements Function<byte[], T> {
    private final Class<T> clazz;
    private final ObjectMapper objectMapper;

    public BaragonDeserializer(ObjectMapper objectMapper, Class<T> clazz) {
      this.clazz = clazz;
      this.objectMapper = objectMapper;
    }

    @Override
    public T apply(byte[] input) {
      try {
        return objectMapper.readValue(input, clazz);
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  private static class UpstreamKeyFunction implements Function<UpstreamInfo, String> {
    @Override
    public String apply(UpstreamInfo input) {
      return input.getUpstream();
    }
  }

  private static final TypeReference<Collection<BaragonServiceState>> BARAGON_SERVICE_STATE_COLLECTION = new TypeReference<Collection<BaragonServiceState>>() {};
}
