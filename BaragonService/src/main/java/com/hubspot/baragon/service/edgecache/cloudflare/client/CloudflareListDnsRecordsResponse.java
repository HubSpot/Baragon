package com.hubspot.baragon.service.edgecache.cloudflare.client;

import java.util.List;

public class CloudflareListDnsRecordsResponse extends CloudflareResponse<List<CloudflareDnsRecord>> {

  public CloudflareListDnsRecordsResponse(Boolean success,
                                          List<CloudflareError> errors,
                                          List<String> messages,
                                          CloudflareResultInfo resultInfo, List<CloudflareDnsRecord> result) {
    super(success, errors, messages, resultInfo, result);
  }
}
