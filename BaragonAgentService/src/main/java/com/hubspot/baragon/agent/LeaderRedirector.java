package com.hubspot.baragon.agent;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import java.net.URI;

@RequestScoped
public class LeaderRedirector {
  private static final Log LOG = LogFactory.getLog(LeaderRedirector.class);
  private final LeaderLatch leaderLatch;
  private final HttpServletRequest request;

  @Inject
  public LeaderRedirector(LeaderLatch leaderLatch, HttpServletRequest request) {
    this.leaderLatch = leaderLatch;
    this.request = request;
  }

  public void redirectToLeader() {
    if (!leaderLatch.hasLeadership()) {
      try {
        throw new WebApplicationException(javax.ws.rs.core.Response
            .temporaryRedirect(URI.create(String.format("http://%s%s%s%s", leaderLatch.getLeader().getId(), request.getContextPath(), request.getServletPath(), request.getPathInfo())))
            .build());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
