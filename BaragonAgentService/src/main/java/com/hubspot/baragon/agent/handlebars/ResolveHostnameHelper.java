package com.hubspot.baragon.agent.handlebars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Optional;
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
      String resolved = resolver.resolveUpstreamDNS(address).or(address);
      LOG.trace("Resolved {} to {}", address, resolved);
      return resolved;
    } else if (upstream instanceof UpstreamInfo) {
      UpstreamInfo upstreamInfo = ((UpstreamInfo) upstream);
      final Optional<String> maybeResolvedUpstream = resolver.resolveUpstreamDNS(upstreamInfo.getUpstream());
      LOG.trace(
          "Trying to resolve an UpstreamInfo upstream of {} with upstreamInfo.getResolvedUpstream() = {}, resolver.resolveUpstreamDNS(upstreamInfo.getUpstream()) = {}, upstreamInfo.getUpstream() = {}",
          upstreamInfo, upstreamInfo.getResolvedUpstream(), maybeResolvedUpstream, upstreamInfo.getUpstream()
      );
      String resolved = upstreamInfo.getResolvedUpstream().or(maybeResolvedUpstream).or(upstreamInfo.getUpstream());
      LOG.trace("Resolved {} to {}", upstreamInfo, resolved);
      return resolved;
    } else {
      LOG.error("resolveHostname called with invalid context {} and options {}", upstream, options);
      throw new IllegalArgumentException(String.format("Don't know how to process upstream %s", upstream.toString()));
    }
  }
}
