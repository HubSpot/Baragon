package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties( ignoreUnknown = true )
public class CloudflarePurgeCacheResponse extends CloudflareResponse<CloudflarePurgeCacheResult> {

  @JsonCreator
  public CloudflarePurgeCacheResponse(@JsonProperty("success") Boolean success,
                                      @JsonProperty("errors") List<CloudflareError> errors,
                                      @JsonProperty("messages") List<String> messages,
                                      @JsonProperty("result_info") CloudflareResultInfo resultInfo,
                                      @JsonProperty("result") CloudflarePurgeCacheResult result) {
    super(success, errors, messages, resultInfo, result);
  }
}
