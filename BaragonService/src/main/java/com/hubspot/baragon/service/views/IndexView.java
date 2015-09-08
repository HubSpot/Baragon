package com.hubspot.baragon.service.views;

import com.hubspot.baragon.service.config.BaragonConfiguration;
import io.dropwizard.views.View;
import static com.google.common.base.Preconditions.checkNotNull;

public class IndexView extends View {

  private final String appRoot;
  private final String staticRoot;
  private final String apiRoot;
  private final String navColor;
  private final Boolean allowEdit;
  private final boolean authEnabled;
  private final String title;
  private final boolean elbEnabled;

  public IndexView(String baragonUriBase, String appRoot, BaragonConfiguration configuration) {
    super("index.mustache");

    checkNotNull(baragonUriBase, "baragonUriBase is null");

    String rawAppRoot = String.format("%s%s", baragonUriBase, appRoot);

    this.appRoot = (rawAppRoot.endsWith("/")) ? rawAppRoot.substring(0, rawAppRoot.length() - 1) : rawAppRoot;
    this.staticRoot = String.format("%s/static", baragonUriBase);
    this.apiRoot = String.format("%s", baragonUriBase);
    this.title = configuration.getUiConfiguration().getTitle();
    this.allowEdit = configuration.getUiConfiguration().allowEdit();
    this.authEnabled = configuration.getAuthConfiguration().isEnabled();
    this.navColor = configuration.getUiConfiguration().getNavColor();
    this.elbEnabled = configuration.getElbConfiguration().isPresent();
  }

  public String getAppRoot() {
    return appRoot;
  }

  public String getStaticRoot() {
    return staticRoot;
  }

  public String getApiRoot() {
    return apiRoot;
  }

  public String getTitle() {
    return title;
  }

  public boolean getAllowEdit() {
    return allowEdit;
  }

  public boolean getAuthEnabled() {
    return authEnabled;
  }

  public String getNavColor() {
    return navColor;
  }

  public boolean getElbEnabled() {
    return elbEnabled;
  }

  @Override
  public String toString() {
    return "IndexView [appRoot=" + appRoot +
      ", staticRoot=" + staticRoot +
      ", apiRoot=" + apiRoot +
      ", authKey=" + authEnabled +
      ", navColor=" + navColor +
      ", readOnly=" + allowEdit +
      ", title=" + title +
      ", elbEnabled" + elbEnabled +
      "]";
  }

}
