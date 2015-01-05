package com.hubspot.baragon.data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Singleton
public class BaragonKnownAgentsDatastore extends AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonKnownAgentsDatastore.class);

  public static final String KNOWN_AGENTS_GROUP_HOSTS_FORMAT = "/load-balancer/%s/known-agents";
  public static final String KNOWN_AGENTS_GROUP_HOST_FORMAT = KNOWN_AGENTS_GROUP_HOSTS_FORMAT + "/%s";

  @Inject
  public BaragonKnownAgentsDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public Collection<BaragonAgentMetadata> getKnownAgentsMetadata(String clusterName) {
    final Collection<String> nodes = getChildren(String.format(KNOWN_AGENTS_GROUP_HOSTS_FORMAT, clusterName));

    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }

    final Collection<BaragonAgentMetadata> metadata = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      try {
        final String value = new String(curatorFramework.getData().forPath(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, node)), Charsets.UTF_8);
        if (value.startsWith("http://")) {
          metadata.add(BaragonAgentMetadata.fromString(value));
        } else {
          metadata.add(objectMapper.readValue(value, BaragonAgentMetadata.class));
        }
      } catch (KeeperException.NoNodeException nne) {
      } catch (JsonParseException | JsonMappingException je) {
        LOG.warn(String.format("Exception deserializing %s", String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, node)), je);
      } catch (InvalidAgentMetadataStringException iamse) {
        LOG.warn(String.format("Exception loading agent metadata from old-style string '%s', ignoring...", iamse.getAgentMetadataString()));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    return metadata;
  }

  public void addKnownAgent(String clusterName, BaragonAgentMetadata agentMetadata) {
    writeToZk(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, agentMetadata.getAgentId()), agentMetadata);
  }

  public void removeKnownAgent(String clusterName, String agentId) {
    deleteNode(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, agentId));
  }

}
