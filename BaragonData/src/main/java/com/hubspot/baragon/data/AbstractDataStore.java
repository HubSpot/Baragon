package com.hubspot.baragon.data;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import com.hubspot.baragon.utils.JavaUtils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.PathAndBytesable;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// because curator is a piece of shit
public abstract class AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStore.class);

  protected final CuratorFramework curatorFramework;
  protected final ObjectMapper objectMapper;

  public static final Comparator<String> SEQUENCE_NODE_COMPARATOR_LOW_TO_HIGH = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      return o1.substring(o1.length()-10).compareTo(o2.substring(o2.length()-10));
    }
  };

  public static final Comparator<String> SEQUENCE_NODE_COMPARATOR_HIGH_TO_LOW = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      return o2.substring(o2.length()-10).compareTo(o1.substring(o1.length()-10));
    }
  };

  public AbstractDataStore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    this.curatorFramework = curatorFramework;
    this.objectMapper = objectMapper;
  }

  protected String encodeUrl(String url) {
    return BaseEncoding.base64Url().encode(url.getBytes(Charsets.UTF_8));
  }

  protected String decodeUrl(String encodedUrl) {
    return new String(BaseEncoding.base64Url().decode(encodedUrl), Charsets.UTF_8);
  }

  protected String sanitizeNodeName(String name) {
    return name.contains("/") ? encodeUrl(name) : name;
  }

  protected boolean nodeExists(String path) {
    final long start = System.currentTimeMillis();

    try {
      Stat stat = curatorFramework.checkExists().forPath(path);
      LOG.trace("Checked node {} in {}", path, JavaUtils.duration(start));
      return stat != null;
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> void writeToZk(String path, T data) {
    final long start = System.currentTimeMillis();

    try {
      final byte[] serializedInfo = objectMapper.writeValueAsBytes(data);

      final PathAndBytesable<?> builder;

      if (curatorFramework.checkExists().forPath(path) != null) {
        builder = curatorFramework.setData();
      } else {
        builder = curatorFramework.create().creatingParentsIfNeeded();
      }

      builder.forPath(path, serializedInfo);
      LOG.debug("Wrote {} bytes in {} ({})", serializedInfo.length, JavaUtils.duration(start), path);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> Optional<T> readFromZk(String path, Class<T> klass) {
    final long start = System.currentTimeMillis();

    try {
      byte[] bytes = curatorFramework.getData().forPath(path);
      LOG.trace("Got {} bytes in {} ({})", bytes.length, JavaUtils.duration(start), path);
      return Optional.of(deserialize(bytes, klass));
    } catch (KeeperException.NoNodeException nne) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> Optional<T> readFromZk(String path, TypeReference<T> typeReference) {
    final long start = System.currentTimeMillis();

    try {
      byte[] bytes = curatorFramework.getData().forPath(path);
      LOG.debug("Got {} bytes in {} ({})", bytes.length, JavaUtils.duration(start), path);
      return Optional.of(deserialize(bytes, typeReference));
    } catch (KeeperException.NoNodeException nne) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> T deserialize(byte[] data, Class<T> klass) {
    try {
      return objectMapper.readValue(data, klass);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> T deserialize(byte[] data, TypeReference<T> typeReference) {
    try {
      return objectMapper.readValue(data, typeReference);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  protected String createNode(String path) {
    try {
      return curatorFramework.create().creatingParentsIfNeeded().forPath(path);
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

  protected <T> String createPersistentSequentialNode(String path, T value) {
    try {
      final byte[] serializedValue = objectMapper.writeValueAsBytes(value);

      return curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path, serializedValue);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

  }

  protected boolean deleteNode(String path) {
    return deleteNode(path, false);
  }

  protected boolean deleteNode(String path, boolean recursive) {
    final long start = System.currentTimeMillis();

    try {
      if (recursive) {
        curatorFramework.delete().deletingChildrenIfNeeded().forPath(path);
        LOG.debug("Finished a recursive delete of {} in {}", path, JavaUtils.duration(start));
      } else {
        curatorFramework.delete().forPath(path);
        LOG.debug("Deleted {} in {}", path, JavaUtils.duration(start));
      }
      return true;
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected List<String> getChildren(String path) {
    final long start = System.currentTimeMillis();

    try {
      List<String> children = curatorFramework.getChildren().forPath(path);
      LOG.trace("Found {} children in {} ({})", children.size(), JavaUtils.duration(start), path);
      return children;
    } catch (KeeperException.NoNodeException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected Optional<Long> getUpdatedAt(String path) {
    final long start = System.currentTimeMillis();

    try {
      Stat stat = curatorFramework.checkExists().forPath(path);
      LOG.trace("Got stats for {} in {}", path, JavaUtils.duration(start));
      return Optional.of(stat.getMtime());
    } catch (KeeperException.NoNodeException e) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
