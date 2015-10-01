package com.hubspot.baragon.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class CachedBaragonState {
  private final byte[] uncompressed;
  private final byte[] gzip;
  private final int version;

  public CachedBaragonState(byte[] uncompressed, int version) {
    this.uncompressed = uncompressed;
    this.gzip = compress(uncompressed);
    this.version = version;
  }

  public byte[] getUncompressed() {
    return uncompressed;
  }

  public byte[] getGzip() {
    return gzip;
  }

  public int getVersion() {
    return version;
  }

  private static byte[] compress(byte[] uncompressed) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try (OutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(uncompressed);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return baos.toByteArray();
  }
}
