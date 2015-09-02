package com.hubspot.baragon.service.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;

import com.google.inject.Singleton;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaragonMappers {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonMappers.class);

  public static class BaragonBytesMapper implements ResultSetMapper<byte[]> {

    @Inject
    public BaragonBytesMapper() {}

    @Override
    public byte[] map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return r.getBytes("bytes");
    }

  }

  public static class BaragonRequestIdMapper implements ResultSetMapper<String> {

    @Inject
    public BaragonRequestIdMapper() {}

    @Override
    public String map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return r.getString("requestId");
    }

  }


}
