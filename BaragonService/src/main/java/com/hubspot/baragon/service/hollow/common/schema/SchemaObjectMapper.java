package com.hubspot.baragon.service.hollow.common.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.netflix.hollow.core.write.HollowWriteStateEngine;

public interface SchemaObjectMapper {
  int writeTo(String schemaTypeName, JsonNode data, HollowWriteStateEngine writeState);
}
