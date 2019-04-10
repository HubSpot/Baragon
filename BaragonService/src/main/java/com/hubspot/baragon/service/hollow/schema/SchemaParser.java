package com.hubspot.baragon.service.hollow.common;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.hubspot.baragon.service.hollow.common.schema.HollowDataReplicationSchema;
import com.netflix.hollow.core.schema.HollowListSchema;
import com.netflix.hollow.core.schema.HollowMapSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema.FieldType;
import com.netflix.hollow.core.schema.HollowSchema;
import com.netflix.hollow.core.schema.HollowSchemaParser;
import com.netflix.hollow.core.schema.HollowSetSchema;

public class SchemaParser {
  private static final Logger LOG = LoggerFactory.getLogger(SchemaParser.class);

  private static final Pattern SYNTAX_PATTERN = Pattern.compile("^\\h*syntax\\h*=\\h*\"(.*?)\"\\h*;\\h*$");

  public static HollowDataReplicationSchema parse(List<String> sources) {
    return new HollowDataReplicationSchema(
        sources.stream()
            .map(SchemaParser::parse)
            .map(HollowDataReplicationSchema::getSchemas)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
  }

  public static HollowDataReplicationSchema parse(String source) {
    int index = source.indexOf(';');
    if (index == -1) {
      throw new IllegalArgumentException("Invalid syntax, expected syntax declaration at start of file");
    }

    String syntaxDeclaration = source.substring(0, index + 1);
    Matcher matcher = SYNTAX_PATTERN.matcher(syntaxDeclaration);
    if (!matcher.matches()) {
      LOG.debug("Expecting syntax declaration for hollow schema, defaulting to \"netflix\"");
      return parseNetflixSchema(source);
    }

    if (matcher.groupCount() != 1) {
      throw new IllegalArgumentException("Invalid syntax, expected syntax declaration at start of file");
    }

    String schema = source.substring(index + 1);
    String syntax = matcher.group(1);
    if (syntax.equalsIgnoreCase("netflix")) {
      return parseNetflixSchema(schema);
    } else if (syntax.equalsIgnoreCase("hubspot-v1")) {
      return parseHubspotSchema(schema);
    } else {
      throw new IllegalArgumentException("Unsupported syntax: " + syntax);
    }
  }

  private static HollowDataReplicationSchema parseNetflixSchema(String source) {
    List<HollowSchema> schemas;
    try {
      schemas = HollowSchemaParser.parseCollectionOfSchemas(source);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new HollowDataReplicationSchema(
        schemas.stream()
            .map(HollowSchemaWrapper::new)
            .collect(Collectors.toList()));
  }

  private static HollowDataReplicationSchema parseHubspotSchema(String source) {
    StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(source));
    tokenizer.wordChars('_', '_');
    tokenizer.slashSlashComments(true);
    tokenizer.slashStarComments(true);

    List<HollowSchemaWrapper> schemaList = new ArrayList<>();

    try {
      List<HollowSchemaWrapper> schema = parseSchema(tokenizer);
      while (!schema.isEmpty()) {
        schemaList.addAll(schema);
        schema = parseSchema(tokenizer);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse schema", e);
    }

    return new HollowDataReplicationSchema(schemaList);
  }

  private static List<HollowSchemaWrapper> parseSchema(StreamTokenizer tokenizer) throws IOException {
    int tok = tokenizer.nextToken();
    if (tok == StreamTokenizer.TT_EOF) {
      return Collections.emptyList();
    }

    while (tok != StreamTokenizer.TT_WORD) {
      if (tok == StreamTokenizer.TT_EOF) {
        return null;
      }

      tok = tokenizer.nextToken();
    }

    String typeName = tokenizer.sval;

    tokenizer.nextToken();
    return parseObjectSchema(typeName, tokenizer);
  }

  private static List<HollowSchemaWrapper> parseObjectSchema(String typeName, StreamTokenizer tokenizer) throws IOException {
    if (tokenizer.ttype != '{') {
      throw new IllegalArgumentException("Invalid syntax: expecting '{' for '" + typeName + "'");
    }

    int tok = tokenizer.nextToken();
    List<List<String>> fieldTokens = new ArrayList<>();

    while (tokenizer.ttype != '}') {
      if (tok != StreamTokenizer.TT_WORD) {
        throw new IllegalArgumentException("Invalid syntax, expected field type: " + typeName);
      }

      List<String> tokens = new ArrayList<>();
      tokens.add(tokenizer.sval);

      tok = tokenizer.nextToken();
      if (tokenizer.ttype == '<') {
        tok = tokenizer.nextToken();
        if (tok != StreamTokenizer.TT_WORD) {
          throw new IllegalArgumentException("Invalid syntax, expected type name after < in field in type: " + typeName);
        }

        tokens.add(tokenizer.sval);
        tok = tokenizer.nextToken();
        if (tokenizer.ttype == ',') {
          tok = tokenizer.nextToken();
          if (tok != StreamTokenizer.TT_WORD) {
            throw new IllegalArgumentException("Invalid syntax, expected type name after , in parameter list in type: " + typeName);
          }

          tokens.add(tokenizer.sval);
          tok = tokenizer.nextToken();
        }

        if (tokenizer.ttype != '>') {
          throw new IllegalArgumentException("Invalid syntax, expected > after parameter type names in type: " + typeName);
        }

        tok = tokenizer.nextToken();
      }

      if (tok != StreamTokenizer.TT_WORD) {
        throw new IllegalArgumentException("Invalid syntax, expected field name: " + typeName);
      }

      String fieldName = tokenizer.sval;
      tokens.add(fieldName);
      tok = tokenizer.nextToken();

      if (tokenizer.ttype != ';') {
        // we could have 3 tokens instead of 2
        fieldName = tokenizer.sval;
        tokens.add(fieldName);
        tokenizer.nextToken();

        if (tokenizer.ttype != ';') {
          throw new IllegalArgumentException("Invalid syntax, expected semicolon: " + typeName + "." + fieldName);
        }
      }

      tok = tokenizer.nextToken();
      fieldTokens.add(tokens);
    }

    return parseTokens(typeName, fieldTokens);
  }

  private static List<HollowSchemaWrapper> parseTokens(String typeName, List<List<String>> fieldTokens) {
    Set<String> wrappedFields = new HashSet<>();
    List<HollowSchemaWrapper> schemas = new ArrayList<>();

    HollowObjectSchema schema = new HollowObjectSchema(typeName, fieldTokens.size());
    for (List<String> tokens : fieldTokens) {
      String start = tokens.get(0);

      if (start.equals("int")) {
        schema.addField(tokens.get(1), FieldType.INT);
      } else if (start.equals("long")) {
        schema.addField(tokens.get(1), FieldType.LONG);
      } else if (start.equals("float")) {
        schema.addField(tokens.get(1), FieldType.FLOAT);
      } else if (start.equals("double")) {
        schema.addField(tokens.get(1), FieldType.DOUBLE);
      } else if (start.equals("boolean")) {
        schema.addField(tokens.get(1), FieldType.BOOLEAN);
      } else if (start.equals("string")) {
        schema.addField(tokens.get(1), FieldType.STRING);
      } else if (start.equals("bytes")) {
        schema.addField(tokens.get(1), FieldType.BYTES);
      } else if (start.equals("List")) {
        HollowSchemaWrapper wrapper = parseListSchema(tokens);
        schema.addField(tokens.get(tokens.size() - 1), FieldType.REFERENCE, wrapper.getName());
        schemas.add(wrapper);
      } else if (start.equals("Set")) {
        HollowSchemaWrapper wrapper = parseSetSchema(tokens);
        schema.addField(tokens.get(tokens.size() - 1), FieldType.REFERENCE, wrapper.getName());
        schemas.add(wrapper);
      } else if (start.equals("Map")) {
        HollowSchemaWrapper wrapper = parseMapSchema(tokens);
        schema.addField(tokens.get(tokens.size() - 1), FieldType.REFERENCE, wrapper.getName());
        schemas.add(wrapper);
      } else {
        schema.addField(tokens.get(tokens.size() - 1), FieldType.REFERENCE, start);
      }
    }

    schemas.add(new HollowSchemaWrapper(schema, wrappedFields));
    return schemas;
  }

  private static HollowSchemaWrapper parseListSchema(List<String> tokens) {
    Preconditions.checkArgument(
        tokens.size() == 3,
        "Expected 3 tokens for List type");

    Preconditions.checkArgument(
        tokens.get(0).equals("List"),
        "Expected first token to be List");

    return new HollowSchemaWrapper(
        new HollowListSchema(
            getTypeName(
                tokens.get(0),
                tokens.get(1),
                tokens.get(2)),
            tokens.get(1)));
  }

  private static HollowSchemaWrapper parseSetSchema(List<String> tokens) {
    Preconditions.checkArgument(
        tokens.size() == 3,
        "Expected 3 tokens for Set type");

    Preconditions.checkArgument(
        tokens.get(0).equals("Set"),
        "Expected first token to be Set");

    return new HollowSchemaWrapper(
        new HollowSetSchema(
            getTypeName(
                tokens.get(0),
                tokens.get(1),
                tokens.get(2)),
            tokens.get(1)));
  }

  private static HollowSchemaWrapper parseMapSchema(List<String> tokens) {
    Preconditions.checkArgument(
        tokens.size() == 4,
        "Expected 3 tokens for Map type");

    Preconditions.checkArgument(
        tokens.get(0).equals("Map"),
        "Expected first token to be Map");


    return new HollowSchemaWrapper(
        new HollowMapSchema(
            getTypeName(
                tokens.get(0),
                tokens.get(1) + tokens.get(2),
                tokens.get(3)),
            tokens.get(1),
            tokens.get(2)));
  }

  private static String getTypeName(String type, String elementType, String fieldName) {
    return String.format(
        "Auto%sOf%s%s",
        type,
        elementType,
        fieldName);
  }

}
