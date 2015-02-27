package com.hubspot.baragon.service.views;

import static com.google.common.base.Preconditions.checkNotNull;

import io.dropwizard.views.View;

import com.hubspot.baragon.service.config.BaragonConfiguration;

public class IndexView extends View {

  private final String appRoot;
  private final String apiDocs;
  private final String staticRoot;
  private final String apiRoot;
  private final String navColor;
  private final Boolean readOnly;

  private final String title;

  public IndexView(String baragonUriBase, String appRoot, BaragonConfiguration configuration) {
    super("index.mustache");

    checkNotNull(baragonUriBase, "baragonUriBase is null");

    String rawAppRoot = String.format("%s%s", baragonUriBase, appRoot);

    this.appRoot = (rawAppRoot.endsWith("/")) ? rawAppRoot.substring(0, rawAppRoot.length() - 1) : rawAppRoot;
    this.staticRoot = String.format("%s/static", baragonUriBase);
    this.apiDocs = String.format("%s/api-docs", baragonUriBase);
    this.apiRoot = String.format("%s", baragonUriBase);
    this.title = configuration.getUiConfiguration().getTitle();
    this.readOnly = configuration.getUiConfiguration().isReadOnly();

    this.navColor = configuration.getUiConfiguration().getNavColor();
  }

  public String getAppRoot() {
    return appRoot;
  }

  public String getStaticRoot() {
    return staticRoot;
  }

  public String getApiDocs() {
    return apiDocs;
  }

  public String getApiRoot() {
    return apiRoot;
  }

  public String getTitle() {
    return title;
  }

  public String getNavColor() {
    return navColor;
  }

  @Override
  public String toString() {
    return "IndexView [appRoot=" + appRoot +
      ", staticRoot=" + staticRoot +
      ", apiRoot=" + apiRoot +
      ", navColor=" + navColor + ", readOnly=" + readOnly +
      ", title=" + title +
      "]";
  }

}
