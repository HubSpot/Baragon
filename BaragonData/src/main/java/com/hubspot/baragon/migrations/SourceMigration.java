package com.hubspot.baragon.migrations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class SourceMigration extends ZkDataMigration {

  private static final Logger LOG = LoggerFactory.getLogger(SourceMigration.class);

  private final BaragonLoadBalancerDatastore baragonLoadBalancerDatastore;
  private final CuratorFramework curatorFramework;
  private final ObjectMapper objectMapper;

  @Inject
  public SourceMigration(BaragonLoadBalancerDatastore baragonLoadBalancerDatastore,
                         ObjectMapper objectMapper,
                         CuratorFramework curatorFramework) {
    super(2);
    this.baragonLoadBalancerDatastore = baragonLoadBalancerDatastore;
    this.objectMapper = objectMapper;
    this.curatorFramework = curatorFramework;
  }

  @Override
  public void applyMigration() {
    try {
      Collection<String> loadBalancerNames = curatorFramework.getChildren().forPath(BaragonLoadBalancerDatastore.LOAD_BALANCER_GROUPS_FORMAT);
      for (String loadBalancerName : loadBalancerNames) {
        String loadBalancerPath = String.format(baragonLoadBalancerDatastore.LOAD_BALANCER_GROUP_FORMAT, loadBalancerName);
        byte[] loadBalancerJson = curatorFramework.getData().forPath(loadBalancerPath);
        ObjectNode loadBalancer = (ObjectNode) objectMapper.readTree(loadBalancerJson);

        // Check if has some sources, if not we can skip it as object doesn't require migrations
        Collection<ObjectNode> sources = new ArrayList<>();
        for (JsonNode sourceName : loadBalancer.path("sources")) {
          if (sourceName.isTextual()) {
            ObjectNode sourceNode = objectMapper.createObjectNode();
            sourceNode.put("name", sourceName.asText());
            sources.add(sourceNode);
          }
        }

        if (sources.isEmpty()) {
          LOG.info(String.format("[%s] -> nothing to migrate", loadBalancerName));
          continue;
        }

        // Create new array of objects [{"name":"orignalName"},...]
        loadBalancer.without("sources").withArray("sources");
        ((ArrayNode) loadBalancer.path("sources")).addAll(sources);

        LOG.info(String.format("[%s] -> %s", loadBalancerName, loadBalancer.toString()));

        curatorFramework.setData()
            .forPath(loadBalancerPath, objectMapper.writeValueAsBytes(loadBalancer));
      }
    }
    catch (KeeperException.NoNodeException e) {
      LOG.info("No nodes for migration");
    }
    catch (Exception e) {
      Throwables.propagate(e);
    }
  }
}
