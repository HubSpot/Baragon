package com.hubspot.baragon.service.config;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;

public class MergingConfigProviderTest {
  private static final String DEFAULT_PATH = "/configs/default.yaml";
  private static final String OVERRIDE_PATH = "/configs/override.yaml";
  private static final String JUST_A_STRING_PATH = "/configs/just_a_string.yaml";
  private static final String DOESNT_EXIST_PATH = "/configs/doesnt_exist.yaml";

  private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

  ObjectMapper objectMapper = Jackson.newObjectMapper()
      .setSerializationInclusion(Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

  @Test
  public void itMergesNestedConfigsProperly() throws Exception {
    InputStream mergedConfigStream = buildConfigSourceProvider(DEFAULT_PATH)
        .open(OVERRIDE_PATH);
    BaragonConfiguration mergedConfig = objectMapper.readValue(
        YAML_FACTORY.createParser(mergedConfigStream),
        BaragonConfiguration.class);

    assertEquals("override-quorum", mergedConfig.getZooKeeperConfiguration().getQuorum());
    assertEquals(100, (int) mergedConfig.getZooKeeperConfiguration().getSessionTimeoutMillis());
    assertEquals("override-namespace", mergedConfig.getZooKeeperConfiguration().getZkNamespace());
    assertEquals(100, (int) mergedConfig.getZooKeeperConfiguration().getConnectTimeoutMillis());
    assertEquals(100, (int) mergedConfig.getZooKeeperConfiguration().getRetryBaseSleepTimeMilliseconds());
    assertEquals(2, mergedConfig.getAgentMaxAttempts());
    assertEquals("127.0.0.1", mergedConfig.getHostname());
    assertEquals("master-auth-key", mergedConfig.getMasterAuthKey());
  }

  @Test(expected = IllegalArgumentException.class)
  public void itThrowsExnOnNonObjectOverride() throws Exception {
    buildConfigSourceProvider(DEFAULT_PATH)
        .open(JUST_A_STRING_PATH);
  }

  @Test(expected = IllegalArgumentException.class)
  public void itThrowsExnOnNonObjectDefault() throws Exception {
    buildConfigSourceProvider(JUST_A_STRING_PATH)
        .open(OVERRIDE_PATH);
  }

  @Test(expected = FileNotFoundException.class)
  public void itThrowsExnOnNotFoundOverridePath() throws Exception {
    buildConfigSourceProvider(DEFAULT_PATH)
        .open(DOESNT_EXIST_PATH);
  }

  @Test(expected = FileNotFoundException.class)
  public void itThrowsExnOnNotFoundDefaultPath() throws Exception {
    buildConfigSourceProvider(DOESNT_EXIST_PATH)
        .open(OVERRIDE_PATH);
  }
}
