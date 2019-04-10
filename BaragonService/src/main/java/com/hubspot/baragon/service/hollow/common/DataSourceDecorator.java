package com.hubspot.baragon.service.hollow.common;

public interface DataSourceDecorator {
  DataSource decorate(DataSource dataSource, NamingStrategy namingStrategy);
}
