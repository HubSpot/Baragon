package com.hubspot.baragon.service.edgecache.cloudflare.client;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public class CloudflareListZonesResponse extends CloudflareResponse<List<CloudflareZone>> {
  public CloudflareListZonesResponse(Boolean success,
                                     List<CloudflareError> errors,
                                     List<String> messages,
                                     CloudflareResultInfo resultInfo,
                                     List<CloudflareZone> result) {
    super(success, errors, messages, resultInfo, result);
  }
}
