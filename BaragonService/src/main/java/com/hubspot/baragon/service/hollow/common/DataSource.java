package com.hubspot.baragon.service.hollow.common;

import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.io.ByteSource;
import com.sun.nio.sctp.NotificationHandler;

public interface DataSource {
  ByteSource getExpected(String name);
  Metadata getMetadataExpected(String name);

  Optional<ByteSource> get(String name);
  Optional<Metadata> getMetadata(String name);

  Iterable<String> list();
  Iterable<String> list(String prefix);

  void set(Metadata blobMetadata, ByteSource source);
  void set(String name, ByteSource source);

  void delete(String name);
  void delete(Iterable<String> names);

  boolean getSupportsPush();

  /**
   * Use {@link DataSource#registerNotificationHandler(NotificationHandler)}
   */
  @Deprecated
  void registerPushHandler(Consumer<String> pushConsumer);

  /**
   * Not supported
   */
  @Deprecated
  void deregisterPushHandler(Consumer<String> pushConsumer);

  void registerNotificationHandler(NotificationHandler handler);
}
