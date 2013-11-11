package com.hubspot.baragon.agent;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import com.sun.jersey.api.core.HttpRequestContext;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import java.net.URI;

@RequestScoped
public class LeaderRedirector {
  private final LeaderLatch leaderLatch;

  @Context
  private HttpRequestContext request;

  @Inject
  public LeaderRedirector(LeaderLatch leaderLatch) {
    this.leaderLatch = leaderLatch;
  }

  public void redirectToLeader() {
    if (!leaderLatch.hasLeadership()) {
      try {
        throw new WebApplicationException(javax.ws.rs.core.Response
            .temporaryRedirect(URI.create(String.format("http://%s%s", leaderLatch.getLeader().getId(), request.getAbsolutePath().getPath())))
            .build());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
