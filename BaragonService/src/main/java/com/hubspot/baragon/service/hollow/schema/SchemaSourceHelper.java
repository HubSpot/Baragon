package com.hubspot.baragon.service.hollow.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import com.hubspot.baragon.service.hollow.common.schema.HollowDataReplicationSchema;

public class SchemaSourceHelper {
  public static SchemaSource fromResources(String... resources) {
    return new ParsingSchemaSource(readResources(resources));
  }

  public static SchemaSource fromStrings(String... strings) {
    return new ParsingSchemaSource(Arrays.asList(strings));
  }

  private static List<String> readResources(String... files) {
    List<String> results = new ArrayList<>();
    for (String file : files) {
      try {
        results.add(Resources.toString(Resources.getResource(file), StandardCharsets.UTF_8));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return results;
  }

  private static class ParsingSchemaSource implements SchemaSource {
    private final HollowDataReplicationSchema schema;
    private final String text;

    public ParsingSchemaSource(List<String> schemaStrings) {
      this.schema = SchemaParser.parse(schemaStrings);
      this.text = Joiner.on(" ").join(schemaStrings);
    }

    @Override
    public Optional<HollowSchemaWrapper> getByName(String name) {
      return schema.get(name);
    }

    @Override
    public Collection<HollowSchemaWrapper> getAll() {
      return schema.getSchemas();
    }

    @Override
    public String getText() {
      return text;
    }
  }

  private SchemaSourceHelper() {
  }

}
