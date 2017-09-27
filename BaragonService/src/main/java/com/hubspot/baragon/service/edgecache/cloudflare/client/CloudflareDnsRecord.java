package com.hubspot.baragon.service.edgecache.cloudflare.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * {
 "id": "372e67954025e0ba6aaa6d586b9e0b59",
 "type": "A",
 "zone_name": "example.com",
 "content": "1.2.3.4",
 "proxiable": true,
 "proxied": false,
 "ttl": 120,
 "locked": false,
 "zone_id": "023e105f4ecef8ad9ca31a8372d0c353",
 "zone_name": "example.com",
 "created_on": "2014-01-01T05:20:00.12345Z",
 "modified_on": "2014-01-01T05:20:00.12345Z",
 "data": {}
 }
 */
@JsonNaming(SnakeCaseStrategy.class)
public class CloudflareDnsRecord {
  private final Boolean proxied;
  private final String zoneName;

  public CloudflareDnsRecord(Boolean proxied, String zoneName) {
    this.proxied = proxied;
    this.zoneName = zoneName;
  }

  public Boolean isProxied() {
    return proxied;
  }

  public String getZoneName() {
    return zoneName;
  }
}
