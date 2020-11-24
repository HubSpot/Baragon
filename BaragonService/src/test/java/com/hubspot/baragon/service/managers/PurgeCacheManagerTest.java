package com.hubspot.baragon.service.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestBuilder;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceBuilder;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.MergingConfigProvider;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;

class PurgeCacheManagerTest {

  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

  private ObjectMapper objectMapper = Jackson.newObjectMapper()
      .setSerializationInclusion(Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private PurgeCacheManager purgeCacheManager;

  private BaragonConfiguration baragonConfiguration;

  private ConfigurationSourceProvider buildConfigSourceProvider(String baseFileName) {
    final Class<?> klass = this.getClass();

    return new MergingConfigProvider(new ConfigurationSourceProvider() {
      @Override
      public InputStream open(String path) throws IOException {
        InputStream inputStream = klass.getResourceAsStream(path);
        if (inputStream == null) {
          throw new FileNotFoundException(String.format("File %s not found in test resources directory", path));
        } else {
          return inputStream;
        }
      }
    }, baseFileName, objectMapper, YAML_FACTORY);
  }

  private BaragonService createService(String serviceId, Optional<String> templateName){
    return new BaragonServiceBuilder().setServiceId(serviceId).setTemplateName(templateName).build();
  }

  private BaragonRequest createRequest(String serviceId, Optional<String> templateName){
    return new BaragonRequestBuilder().setAction(Optional.of(RequestAction.UPDATE)).setLoadBalancerService(
        createService(serviceId, templateName)
    ).build();
  }

  @BeforeEach
  public void before() throws IOException {
    InputStream mergedConfigStream = buildConfigSourceProvider("/configs/default.yaml")
        .open("/configs/purgeCache.yaml");
    baragonConfiguration = objectMapper.readValue(
        YAML_FACTORY.createParser(mergedConfigStream),
        BaragonConfiguration.class);
    purgeCacheManager = new PurgeCacheManager(baragonConfiguration);
  }

  @Test
  public void testPurgeCacheUpdate() {
    // 1. serviceId is not excluded, and the template name is not eligible for the update to isPurgeCache=true
    // so nothing should change here
    BaragonRequest request = createRequest("madeUpServiceId", Optional.absent());
    request = purgeCacheManager.updateForPurgeCache(request);
    assertEquals(false, request.isPurgeCache());
  }

  @Test
  public void testPurgeCacheUpdateWithIncludedGroup() {
    // 2. static_routes is eligible for the update to isPurgeCache=true, so test that it is updated
    BaragonRequest request = createRequest("madeUpServiceID", Optional.of("static_routes"));
    request = purgeCacheManager.updateForPurgeCache(request);
    assertEquals(true, request.isPurgeCache());
  }

  @Test
  public void testPurgeCacheUpdateWithIncludedGroupButExcludedServiceId() {
    // 3. while static_routes is eligible for the update to isPurgeCache=true, excludedServiceId is part of the
    // excludedServiceIds list, so the action for this request should not be updated
    BaragonRequest request = createRequest("excludedServiceId", Optional.of("static_routes"));
    request = purgeCacheManager.updateForPurgeCache(request);
    assertEquals(false, request.isPurgeCache());
  }

  @Test
  public void testAlternatePurgeCacheConfig() throws IOException {
    // 4. try a configuration without a purgeCache#excludedServiceIds list
    InputStream mergedConfigStream = buildConfigSourceProvider("/configs/default.yaml")
        .open("/configs/purgeCache-alternate.yaml");
    baragonConfiguration = objectMapper.readValue(
        YAML_FACTORY.createParser(mergedConfigStream),
        BaragonConfiguration.class);
    purgeCacheManager = new PurgeCacheManager(baragonConfiguration);

    // 4a. in the test above, this request was not eligible for the update to isPurgeCache=true, however, this
    // new config doesn't have an excludedServiceIds block, so this request should be updated
    BaragonRequest request = createRequest("excludedServiceId", Optional.of("static_routes"));
    request = purgeCacheManager.updateForPurgeCache(request);
    assertEquals(true, request.isPurgeCache());
  }

}