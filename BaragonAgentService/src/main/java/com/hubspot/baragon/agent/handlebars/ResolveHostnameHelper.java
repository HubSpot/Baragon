package com.hubspot.baragon.agent.handlebars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.utils.UpstreamResolver;

public class ResolveHostnameHelper implements Helper<Object> {
  private static final Logger LOG = LoggerFactory.getLogger(ResolveHostnameHelper.class);

  public static final String NAME = "resolveHostname";

  private final UpstreamResolver resolver;

  public ResolveHostnameHelper(UpstreamResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public CharSequence apply(Object upstream, Options options) {
    if (upstream instanceof String) {
      String address = ((String) upstream);
      LOG.trace("Trying to resolve a String upstream of {}", address);
      return resolver.resolveUpstreamDNS(address).or(address);
    } else if (upstream instanceof UpstreamInfo) {
      UpstreamInfo upstreamInfo = ((UpstreamInfo) upstream);
      LOG.trace("Trying to resolve an UpstreamInfo upstream of {}", upstreamInfo);
      return upstreamInfo.getResolvedUpstream().or(resolver.resolveUpstreamDNS(upstreamInfo.getUpstream())).or(upstreamInfo.getUpstream());
    } else {
      LOG.error("resolveHostname called with invalid context {} and options {}", upstream, options);
      throw new IllegalArgumentException(String.format("Don't know how to process upstream %s", upstream.toString()));
    }
  }
}
