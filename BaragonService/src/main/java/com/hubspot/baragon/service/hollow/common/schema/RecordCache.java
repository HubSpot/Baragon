package com.hubspot.baragon.service.hollow;

import java.util.HashMap;
import java.util.Map;

import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.core.write.HollowListWriteRecord;
import com.netflix.hollow.core.write.HollowMapWriteRecord;
import com.netflix.hollow.core.write.HollowObjectWriteRecord;
import com.netflix.hollow.core.write.HollowSetWriteRecord;

public class RecordCache {
  private static final ThreadLocal<RecordCache> CACHE = ThreadLocal.withInitial(RecordCache::new);

  private final Map<HollowObjectSchema, HollowObjectWriteRecord> objectRecordCache = new HashMap<>();
  private HollowListWriteRecord listWriteRecord;
  private HollowSetWriteRecord setWriteRecord;
  private HollowMapWriteRecord mapWriteRecord;

  public static HollowObjectWriteRecord getObjectRecordFor(HollowObjectSchema schema) {
    return CACHE.get().fetchObjectRecordFor(schema);
  }

  public static HollowListWriteRecord getListWriteRecord() {
    return CACHE.get().fetchListWriteRecord();
  }

  public static HollowSetWriteRecord getSetWriteRecord() {
    return CACHE.get().fetchSetWriteRecord();
  }

  public static HollowMapWriteRecord getMapWriteRecord() {
    return CACHE.get().fetchMapWriteRecord();
  }


  private HollowObjectWriteRecord fetchObjectRecordFor(HollowObjectSchema schema) {
    HollowObjectWriteRecord record = objectRecordCache.computeIfAbsent(schema, HollowObjectWriteRecord::new);
    record.reset();
    return record;
  }

  private HollowListWriteRecord fetchListWriteRecord() {
    if (listWriteRecord == null) {
      listWriteRecord = new HollowListWriteRecord();
    }

    listWriteRecord.reset();
    return listWriteRecord;
  }

  private HollowSetWriteRecord fetchSetWriteRecord() {
    if (setWriteRecord == null) {
      setWriteRecord = new HollowSetWriteRecord();
    }

    setWriteRecord.reset();
    return setWriteRecord;
  }

  private HollowMapWriteRecord fetchMapWriteRecord() {
    if (mapWriteRecord == null) {
      mapWriteRecord = new HollowMapWriteRecord();
    }

    mapWriteRecord.reset();
    return mapWriteRecord;
  }

  private RecordCache() {
  }
}
