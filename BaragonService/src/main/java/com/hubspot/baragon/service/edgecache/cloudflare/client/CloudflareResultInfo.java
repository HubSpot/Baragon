package com.hubspot.baragon.service.edgecache.cloudflare.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudflareResultInfo {
  private final Integer page;
  private final Integer perPage;
  private final Integer count;
  private final Integer totalCount;
  private final Integer totalPages;

  @JsonCreator
  public CloudflareResultInfo(@JsonProperty("page") Integer page,
                              @JsonProperty("per_page") Integer perPage,
                              @JsonProperty("count") Integer count,
                              @JsonProperty("total_count") Integer totalCount,
                              @JsonProperty("total_pages") Integer totalPages) {
    this.page = page;
    this.perPage = perPage;
    this.count = count;
    this.totalCount = totalCount;
    this.totalPages = totalPages;
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

  public Integer getTotalPages() {
    return totalPages;
  }
}
