package com.hubspot.baragon.service.edgecache.cloudflare;

import com.hubspot.baragon.service.edgecache.EdgeCache;

public enum EdgeCacheClass {
  CLOUDFLARE(CloudflareEdgeCache.class);

  private final Class<? extends EdgeCache> edgeCacheClass;

  EdgeCacheClass(Class<? extends EdgeCache> edgeCacheClass) {
    this.edgeCacheClass = edgeCacheClass;
  }

  public Class<? extends EdgeCache> getEdgeCacheClass() {
    return edgeCacheClass;
  }

}
