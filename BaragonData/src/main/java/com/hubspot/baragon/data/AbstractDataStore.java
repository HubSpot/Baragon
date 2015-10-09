package com.hubspot.baragon.data;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.PathAndBytesable;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.utils.JavaUtils;

// because curator is a piece of shit
public abstract class AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStore.class);

  public enum OperationType {
    READ,
    WRITE;
  }

  protected final CuratorFramework curatorFramework;
  protected final ObjectMapper objectMapper;
  protected final ZooKeeperConfiguration zooKeeperConfiguration;

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

  public AbstractDataStore(CuratorFramework curatorFramework, ObjectMapper objectMapper, ZooKeeperConfiguration zooKeeperConfiguration) {
    this.curatorFramework = curatorFramework;
    this.objectMapper = objectMapper;
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

  protected void log(OperationType type, Optional<Integer> numItems, Optional<Integer> bytes, long start, String path) {
    final String message = String.format("%s (items: %s) (bytes: %s) in %s (%s)", type, numItems.or(1), bytes.or(0), JavaUtils.duration(start), path);

    final long duration = System.currentTimeMillis() - start;

    if ((bytes.isPresent() && bytes.get() > zooKeeperConfiguration.getDebugCuratorCallOverBytes()) || (duration > zooKeeperConfiguration.getDebugCuratorCallOverMillis())) {
      LOG.debug(message);
    } else {
      LOG.trace(message);
    }
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
      log(OperationType.READ, Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
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
      final byte[] serializedInfo = serialize(data);

      final PathAndBytesable<?> builder;

      if (curatorFramework.checkExists().forPath(path) != null) {
        builder = curatorFramework.setData();
      } else {
        builder = curatorFramework.create().creatingParentsIfNeeded();
      }

      builder.forPath(path, serializedInfo);
      log(OperationType.WRITE, Optional.<Integer>absent(), Optional.of(serializedInfo.length), start, path);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> byte[] serialize(T data) {
    try {
      return objectMapper.writeValueAsBytes(data);
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> Optional<T> readFromZk(final String path, final Class<T> klass) {
    final long start = System.currentTimeMillis();

    return readFromZk(path).transform(new Function<byte[], T>() {

      @Override
      public T apply(byte[] data) {
        log(OperationType.READ, Optional.<Integer>absent(), Optional.of(data.length), start, path);
        return deserialize(data, klass);
      }
    });
  }

  protected Optional<byte[]> readFromZk(String path) {
    try {
      return Optional.of(curatorFramework.getData().forPath(path));
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

  protected String createPersistentSequentialNode(String path) {
    final long start = System.currentTimeMillis();

    try {
      final String result = curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path);
      log(OperationType.WRITE, Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
      return result;
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
        log(OperationType.WRITE, Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
      } else {
        curatorFramework.delete().forPath(path);
        log(OperationType.WRITE, Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
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
      log(OperationType.READ, Optional.of(children.size()), Optional.<Integer>absent(), start, path);
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
      log(OperationType.READ, Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
      return Optional.of(stat.getMtime());
    } catch (KeeperException.NoNodeException e) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
