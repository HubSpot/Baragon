package com.hubspot.baragon.service.hollow.common;

import java.util.Collection;
import java.util.Optional;

public interface SchemaSource {

  static SchemaSource fromResources(String... resources) {
    return SchemaSourceHelper.fromResources(resources);
  }

  static SchemaSource fromStrings(String... strings) {
    return SchemaSourceHelper.fromStrings(strings);
  }

  Optional<HollowSchemaWrapper> getByName(String name);
  Collection<HollowSchemaWrapper> getAll();
  String getText();
}
