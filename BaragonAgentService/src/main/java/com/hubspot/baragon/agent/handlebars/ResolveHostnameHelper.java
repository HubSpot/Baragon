package com.hubspot.baragon.agent.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.utils.UpstreamResolver;

public class ResolveHostnameHelper implements Helper<Object> {

  public static final String NAME = "resolveHostname";

  private final UpstreamResolver resolver;

  public ResolveHostnameHelper(UpstreamResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public CharSequence apply(Object upstream, Options options) {
    if (upstream instanceof String) {
      String address = ((String) upstream);
      return resolver.resolveUpstreamDNS(address).or(address);
    } else if (upstream instanceof UpstreamInfo) {
      UpstreamInfo upstreamInfo = ((UpstreamInfo) upstream);
      return upstreamInfo.getResolvedUpstream().or(resolver.resolveUpstreamDNS(upstreamInfo.getUpstream())).or(upstreamInfo.getUpstream());
    } else {
      throw new IllegalArgumentException(String.format("Don't know how to process upstream %s", upstream.toString()));
    }
  }
}
