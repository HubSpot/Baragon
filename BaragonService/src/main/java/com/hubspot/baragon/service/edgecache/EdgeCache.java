package com.hubspot.baragon.service.edgecache;

import com.hubspot.baragon.models.BaragonRequest;

public interface EdgeCache {

  boolean invalidateIfNecessary(BaragonRequest request);

}
