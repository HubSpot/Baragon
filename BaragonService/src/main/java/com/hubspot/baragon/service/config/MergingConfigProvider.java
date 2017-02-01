package com.hubspot.baragon.service.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.dropwizard.configuration.ConfigurationSourceProvider;

public final class MergingConfigProvider implements ConfigurationSourceProvider {
  private final ConfigurationSourceProvider delegate;
  private final String defaultConfigPath;
  private final ObjectMapper objectMapper;
  private final YAMLFactory yamlFactory;

  public MergingConfigProvider(ConfigurationSourceProvider delegate, String defaultConfigPath, ObjectMapper objectMapper, YAMLFactory yamlFactory) {
    this.delegate = delegate;
    this.defaultConfigPath = defaultConfigPath;
    this.objectMapper = objectMapper;
    this.yamlFactory = yamlFactory;
  }

  @Override
  public InputStream open(String path) throws IOException {
    JsonNode originalNode = readFromPath(defaultConfigPath);
    JsonNode overrideNode = readFromPath(path);

    if (originalNode.isObject() && overrideNode.isObject()) {
      ObjectNode original = (ObjectNode) originalNode;
      ObjectNode override = (ObjectNode) overrideNode;

      merge(original, override);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      objectMapper.writeTree(yamlFactory.createGenerator(outputStream), original);

      return new ByteArrayInputStream(outputStream.toByteArray());
    } else {
      throw new IllegalArgumentException("Both configuration files must be objects");
    }
  }

  private void merge(ObjectNode original, ObjectNode override) {
    Iterator<String> fieldNames = override.fieldNames();
    while (fieldNames.hasNext()) {
      String overrideKey = fieldNames.next();
      if (original.get(overrideKey) == null || original.get(overrideKey).isNull()) {
        original.set(overrideKey, override.get(overrideKey));
      } else if (original.get(overrideKey).isObject()
          && override.get(overrideKey).isObject()) {
        merge((ObjectNode) original.get(overrideKey), (ObjectNode) override.get(overrideKey));
      } else {
        original.set(overrideKey, override.get(overrideKey));
      }
    }
  }

  private JsonNode readFromPath(String path) throws IOException {
    return objectMapper.readTree(yamlFactory.createParser(delegate.open(path)));
  }
}
