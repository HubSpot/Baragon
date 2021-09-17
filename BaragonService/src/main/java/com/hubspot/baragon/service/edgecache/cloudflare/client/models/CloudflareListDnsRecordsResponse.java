package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudflareListDnsRecordsResponse
  extends CloudflareResponse<Set<CloudflareDnsRecord>> {

  @JsonCreator
  public CloudflareListDnsRecordsResponse(
    @JsonProperty("success") Boolean success,
    @JsonProperty("errors") List<CloudflareError> errors,
    @JsonProperty("messages") List<String> messages,
    @JsonProperty("result_info") CloudflareResultInfo resultInfo,
    @JsonProperty("result") Set<CloudflareDnsRecord> result
  ) {
    super(success, errors, messages, resultInfo, result);
  }
}
