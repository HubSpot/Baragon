package com.hubspot.baragon.cache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class CachedBaragonState {
  private final byte[] uncompressed;
  private final byte[] gzip;
  private final int version;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public CachedBaragonState(byte[] uncompressed, int version) {
    this.uncompressed = uncompressed;
    this.gzip = compress(uncompressed);
    this.version = version;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getUncompressed() {
    return uncompressed;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
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
