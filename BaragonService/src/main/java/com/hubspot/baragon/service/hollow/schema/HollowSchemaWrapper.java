package com.hubspot.baragon.service.hollow.schema;

import java.util.Collections;
import java.util.Set;

import com.netflix.hollow.core.schema.HollowSchema;
import com.netflix.hollow.core.schema.HollowSchema.SchemaType;

public class HollowSchemaWrapper {
  private final HollowSchema schema;
  private final Set<String> wrapperFields;

  public HollowSchemaWrapper(HollowSchema schema) {
    this(schema, Collections.emptySet());
  }

  public HollowSchemaWrapper(HollowSchema schema,
                             Set<String> wrapperFields) {
    this.schema = schema;
    this.wrapperFields = wrapperFields;
  }

  public HollowSchema getHollowSchema() {
    return schema;
  }

  public String getName() {
    return getHollowSchema().getName();
  }

  public SchemaType getType() {
    return getHollowSchema().getSchemaType();
  }
}
