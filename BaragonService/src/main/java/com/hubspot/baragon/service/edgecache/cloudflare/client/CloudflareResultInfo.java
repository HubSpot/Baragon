package com.hubspot.baragon.service.edgecache.cloudflare.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public class CloudflareResultInfo {
  private final Integer page;
  private final Integer perPage;
  private final Integer count;
  private final Integer totalCount;

  public CloudflareResultInfo(Integer page, Integer perPage, Integer count, Integer totalCount) {
    this.page = page;
    this.perPage = perPage;
    this.count = count;
    this.totalCount = totalCount;
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPerPage() {
    return perPage;
  }

  public Integer getCount() {
    return count;
  }

  public Integer getTotalCount() {
    return totalCount;
  }
}
