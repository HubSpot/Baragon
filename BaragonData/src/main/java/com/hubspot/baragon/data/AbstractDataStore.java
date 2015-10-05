package com.hubspot.baragon.data;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.PathAndBytesable;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import com.hubspot.baragon.utils.JavaUtils;

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

  protected void log(String type, Optional<Integer> numItems, Optional<Integer> bytes, long start, String path) {
    LOG.debug(String.format("%s (items: %s) (bytes: %s) in %s (%s)", type, numItems.or(1), bytes.or(0), JavaUtils.duration(start), path));
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
      log("Fetched", Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
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
      log("Saved", Optional.<Integer>absent(), Optional.of(serializedInfo.length), start, path);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> Optional<T> readFromZk(final String path, final Class<T> klass) {
    final long start = System.currentTimeMillis();

    return readFromZk(path).transform(new Function<byte[], T>() {

      @Override
      public T apply(byte[] data) {
        log("Fetched", Optional.<Integer>absent(), Optional.of(data.length), start, path);
        return deserialize(data, klass);
      }
    });
  }

  protected <T> Optional<T> readFromZk(final String path, final TypeReference<T> typeReference) {
    final long start = System.currentTimeMillis();

    return readFromZk(path).transform(new Function<byte[], T>() {

      @Override
      public T apply(byte[] data) {
        log("Fetched", Optional.<Integer>absent(), Optional.of(data.length), start, path);
        return deserialize(data, typeReference);
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

  protected <T> T deserialize(byte[] data, TypeReference<T> typeReference) {
    try {
      return objectMapper.readValue(data, typeReference);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  protected String createNode(String path) {
    final long start = System.currentTimeMillis();

    try {
      final String result = curatorFramework.create().creatingParentsIfNeeded().forPath(path);
      log("Created", Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
      return result;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected String createPersistentSequentialNode(String path) {
    final long start = System.currentTimeMillis();

    try {
      final String result = curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path);
      log("Created", Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
      return result;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> String createPersistentSequentialNode(String path, T value) {
    final long start = System.currentTimeMillis();

    try {
      final byte[] serializedValue = objectMapper.writeValueAsBytes(value);
      final String result = curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path, serializedValue);
      log("Created", Optional.<Integer>absent(), Optional.of(serializedValue.length), start, path);
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
        log("Deleted", Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
      } else {
        curatorFramework.delete().forPath(path);
        log("Deleted", Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
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
      log("Fetched", Optional.of(children.size()), Optional.<Integer>absent(), start, path);
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
      log("Fetched", Optional.<Integer>absent(), Optional.<Integer>absent(), start, path);
      return Optional.of(stat.getMtime());
    } catch (KeeperException.NoNodeException e) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
