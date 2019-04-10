package com.hubspot.baragon.service.hollow.common.schema;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.hubspot.baragon.service.hollow.common.HollowSchemaWrapper;

public class HollowDataReplicationSchema {
  private final Map<String, HollowSchemaWrapper> schemas;

  public HollowDataReplicationSchema(List<HollowSchemaWrapper> schemas) {
    Preconditions.checkArgument(!schemas.isEmpty(), "Schemas must not be empty");
    this.schemas = schemas.stream()
        .collect(Collectors.toMap(
            HollowSchemaWrapper::getName,
            Function.identity()));
  }

  public Optional<HollowSchemaWrapper> get(String name) {
    return Optional.ofNullable(schemas.get(name));
  }

  public Collection<HollowSchemaWrapper> getSchemas() {
    return schemas.values();
  }
}
