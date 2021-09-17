package com.hubspot.baragon.agent;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.server.Server;

public class ServerProvider
  implements Provider<Optional<Server>>, ServerLifecycleListener {
  private final AtomicReference<Server> serverHolder = new AtomicReference<>();

  @Inject
  public ServerProvider() {}

  @Override
  public Optional<Server> get() {
    return Optional.fromNullable(serverHolder.get());
  }

  @Override
  public void serverStarted(Server server) {
    serverHolder.set(server);
  }
}
