package com.hubspot.baragon.service.hollow.common.schema;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hubspot.baragon.service.hollow.schema.HollowSchemaWrapper;
import com.hubspot.baragon.service.hollow.schema.SchemaSource;
import com.netflix.hollow.core.schema.HollowListSchema;
import com.netflix.hollow.core.schema.HollowMapSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema.FieldType;
import com.netflix.hollow.core.schema.HollowSchema;
import com.netflix.hollow.core.schema.HollowSetSchema;
import com.netflix.hollow.core.write.HollowListWriteRecord;
import com.netflix.hollow.core.write.HollowObjectWriteRecord;
import com.netflix.hollow.core.write.HollowSetWriteRecord;
import com.netflix.hollow.core.write.HollowWriteStateEngine;

public class StdSchemaObjectMapper implements SchemaObjectMapper {
  private static final Logger LOG = LoggerFactory.getLogger(StdSchemaObjectMapper.class);
  public static final String WRAPPED_FIELD = "_wrappedValue";
  public static final String WRAPPED_JSON_FIELD = "_wrappedJsonValue";

  private final SchemaSource schemaSource;
  private final ObjectMapper mapper;

  public StdSchemaObjectMapper(SchemaSource schemaSource,
                               ObjectMapper mapper) {
    this.schemaSource = schemaSource;
    this.mapper = mapper;
  }

  @Override
  public int writeTo(String schemaTypeName, JsonNode data, HollowWriteStateEngine writeState) {
    Preconditions.checkNotNull(data, "Cannot write null!");
    try {
      return writeTo(getSchema(schemaTypeName), data, writeState);
    } catch (Exception e) {
      LOG.debug("Failed to map provided object to schema: {}");
      throw e;
    }
  }

  private int writeTo(HollowSchema schema, JsonNode src, HollowWriteStateEngine writeState) {
    switch (schema.getSchemaType()) {
      case OBJECT:
        return writeObjectTo((HollowObjectSchema) schema, src, writeState);
      case MAP:
        return writeMap((HollowMapSchema) schema, src, writeState);
      case SET:
        return writeSet((HollowSetSchema) schema, src, writeState);
      case LIST:
        return writeList((HollowListSchema) schema, src, writeState);
      default:
        throw new IllegalArgumentException("Unknown schema type " + schema.getSchemaType());
    }
  }

  private int writeObjectTo(HollowObjectSchema schema, JsonNode src, HollowWriteStateEngine writeState) {
    HollowObjectWriteRecord record = RecordCache.getObjectRecordFor(schema);
    if (isFieldWrapper(schema, src)) {
      // handle reference lifting
      writeObjectField(
          schema,
          schema.getFieldName(0),
          schema.getFieldType(0),
          src,
          record,
          writeState);
    } else if (isJsonFieldWrapper(schema)) {
      // handle json reference lifting
      writeJsonObjectField(
          schema.getFieldName(0),
          src,
          record);
    } else {
      // handle standard objects
      for (int i = 0; i < schema.numFields(); ++i) {
        String fieldName = schema.getFieldName(i);
        FieldType type = schema.getFieldType(i);
        JsonNode field = src.get(fieldName);
        if (field == null) {
          LOG.debug("Found missing field: {}.{}, writing null", schema.getName(), fieldName);
          continue;
        }

        writeObjectField(
            schema,
            fieldName,
            type,
            field,
            record,
            writeState);
      }
    }

    return writeState.add(schema.getName(), record);
  }

  private boolean isFieldWrapper(HollowObjectSchema schema, JsonNode src) {
    return schema.numFields() == 1
        && schema.getFieldName(0).equals(WRAPPED_FIELD)
        && src.isValueNode();
  }

  private boolean isJsonFieldWrapper(HollowObjectSchema schema) {
    return schema.numFields() == 1
        && schema.getFieldName(0).equals(WRAPPED_JSON_FIELD)
        && schema.getFieldType(0) == FieldType.STRING;
  }

  private void writeJsonObjectField(String fieldName, JsonNode field, HollowObjectWriteRecord record) {
    if (field.isNull()) {
      return;
    }

    try {
      record.setString(fieldName, mapper.writeValueAsString(field));
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert JsonNode to string", e);
    }
  }

  private void writeObjectField(HollowObjectSchema schema, String fieldName, FieldType type, JsonNode field, HollowObjectWriteRecord record, HollowWriteStateEngine writeState) {
    if (field.isNull()) {
      return;
    }

    switch (type) {
      case INT:
        Preconditions.checkArgument(
            field.canConvertToInt(),
            "Expected type convertable to int for field: %s. Got %s instead with value %s",
            fieldName,
            field.getNodeType(),
            field.toString());

        record.setInt(fieldName, field.intValue());
        break;
      case LONG:
        Preconditions.checkArgument(
            field.canConvertToLong(),
            "Expected type convertable to long for field: %s. Got %s instead with value %s",
            fieldName,
            field.getNodeType(),
            field.toString());

        record.setLong(fieldName, field.longValue());
        break;
      case BYTES:
        Preconditions.checkArgument(
            field.isBinary(),
            "Expected type binary for field: %s. Got %s instead with value %s",
            fieldName,
            field.getNodeType(),
            field.toString());

        try {
          record.setBytes(fieldName, field.binaryValue());
        } catch (Exception e) {
          throw new RuntimeException("Failed to read field binary value", e);
        }
        break;
      case FLOAT:
        Preconditions.checkArgument(
            field.isNumber(),
            "Expected type number for field: %s. Got %s instead with value %s",
            fieldName,
            field.getNodeType(),
            field.toString());

        record.setFloat(fieldName, field.floatValue());
        break;
      case DOUBLE:
        Preconditions.checkArgument(
            field.isNumber(),
            "Expected type number for field: %s. Got %s instead with value %s",
            fieldName,
            field.getNodeType(),
            field.toString());

        record.setDouble(fieldName, field.doubleValue());
        break;
      case STRING:
        Preconditions.checkArgument(
            field.isTextual(),
            "Expected type string for field: %s. Got %s instead with value %s",
            fieldName,
            field.getNodeType(),
            field.toString());

        record.setString(fieldName, field.textValue());
        break;
      case BOOLEAN:
        Preconditions.checkArgument(
            field.isBoolean(),
            "Expected type boolean for field: %s. Got %s instead with value %s",
            fieldName,
            field.getNodeType(),
            field.toString());

        record.setBoolean(fieldName, field.booleanValue());
        break;
      case REFERENCE:
        HollowSchema referencedSchema = getSchema(schema.getReferencedType(fieldName));
        record.setReference(fieldName, writeTo(referencedSchema, field, writeState));
        break;
      default:
        throw new RuntimeException("Unknown field type " + type.name());
    }
  }

  private int writeMap(HollowMapSchema schema, JsonNode data, HollowWriteStateEngine writeState) {
    throw new UnsupportedOperationException("Map types are not supported");
  }

  private int writeSet(HollowSetSchema schema, JsonNode data, HollowWriteStateEngine writeState) {
    HollowSetWriteRecord record = RecordCache.getSetWriteRecord();
    Preconditions.checkArgument(data.isArray(), "Data is not a set type");

    Iterator<JsonNode> elements = data.elements();
    while (elements.hasNext()) {
      JsonNode element = elements.next();
      Preconditions.checkNotNull(element, "Set types do not support null values");

      int ordinal = writeTo(schema.getElementType(), element, writeState);
      int hashCode = writeState.getHashCodeFinder().hashCode(schema.getElementType(), ordinal, element);
      record.addElement(ordinal, hashCode);
    }

    return writeState.add(schema.getName(), record);
  }

  private int writeList(HollowListSchema schema, JsonNode data, HollowWriteStateEngine writeState) {
    HollowListWriteRecord record = RecordCache.getListWriteRecord();
    Preconditions.checkArgument(data.isArray(), "Data is not a list type");

    Iterator<JsonNode> elements = data.elements();
    while (elements.hasNext()) {
      JsonNode element = elements.next();
      Preconditions.checkNotNull(element, "List types do not support null values");

      record.addElement(writeTo(schema.getElementType(), element, writeState));
    }

    return writeState.add(schema.getName(), record);
  }

  private HollowSchema getSchema(String name) {
    return schemaSource.getByName(name)
        .map(HollowSchemaWrapper::getHollowSchema)
        .orElseThrow(() -> new IllegalArgumentException("No schema available for name " + name));
  }
}
