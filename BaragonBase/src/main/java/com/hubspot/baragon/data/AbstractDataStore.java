package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.PathAndBytesable;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.util.Collections;
import java.util.List;

// because curator is a piece of shit
public abstract class AbstractDataStore {
  protected final CuratorFramework curatorFramework;
  protected final ObjectMapper objectMapper;

  public AbstractDataStore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    this.curatorFramework = curatorFramework;
    this.objectMapper = objectMapper;
  }

  protected boolean nodeExists(String path) {
    try {
      return curatorFramework.checkExists().forPath(path) != null;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> void writeToZk(String path, T data) {
    try {
      final byte[] serializedInfo = objectMapper.writeValueAsBytes(data);

      final PathAndBytesable<?> builder;

      if (curatorFramework.checkExists().forPath(path) != null) {
        builder = curatorFramework.setData();
      } else {
        builder = curatorFramework.create().creatingParentsIfNeeded();
      }

      builder.forPath(path, serializedInfo);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> Optional<T> readFromZk(String path, Class<T> klass) {
    try {
      return Optional.of(objectMapper.readValue(curatorFramework.getData().forPath(path), klass));
    } catch (KeeperException.NoNodeException nne) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected boolean createNode(String path) {
    try {
      curatorFramework.create().creatingParentsIfNeeded().forPath(path);
      return true;
    } catch (KeeperException.NodeExistsException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected String createPersistentSequentialNode(String path) {
    try {
      return curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected boolean deleteNode(String path) {
    try {
      curatorFramework.delete().forPath(path);
      return true;
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected List<String> getChildren(String path) {
    try {
      return curatorFramework.getChildren().forPath(path);
    } catch (KeeperException.NoNodeException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
