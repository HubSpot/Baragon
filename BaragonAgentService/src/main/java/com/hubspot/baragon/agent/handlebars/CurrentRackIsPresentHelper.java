package com.hubspot.baragon.agent.handlebars;

import java.io.IOException;
import java.util.Collection;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Optional;
import com.hubspot.baragon.models.UpstreamInfo;

public class CurrentRackIsPresentHelper implements Helper<Collection<UpstreamInfo>> {
  public static final String NAME = "currentRackIsPresent";

  private final Optional<String> currentRackId;

  public CurrentRackIsPresentHelper(Optional<String> currentRackId) {
    this.currentRackId = currentRackId;
  }

  @Override
  public CharSequence apply(Collection<UpstreamInfo> upstreams, Options options) throws IOException {
    if (!currentRackId.isPresent()) {
      return options.fn();
    }

    if (upstreams == null) {
      return options.inverse();
    }

    for (UpstreamInfo upstreamInfo : upstreams) {
      if (upstreamInfo.getRackId().isPresent() && upstreamInfo.getRackId().get().toLowerCase().equals(currentRackId.get().toLowerCase())) {
        return options.fn();
      }
    }
    return options.inverse();
  }
}
