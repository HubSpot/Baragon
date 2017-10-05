package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloudflareResultInfo that = (CloudflareResultInfo) o;
    return Objects.equal(page, that.page) &&
        Objects.equal(perPage, that.perPage) &&
        Objects.equal(count, that.count) &&
        Objects.equal(totalCount, that.totalCount) &&
        Objects.equal(totalPages, that.totalPages);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(page, perPage, count, totalCount, totalPages);
  }

  @Override
  public String toString() {
    return "CloudflareResultInfo{" +
        "page=" + page +
        ", perPage=" + perPage +
        ", count=" + count +
        ", totalCount=" + totalCount +
        ", totalPages=" + totalPages +
        '}';
  }
}
