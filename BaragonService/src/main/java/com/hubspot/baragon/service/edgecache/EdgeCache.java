package com.hubspot.baragon.service.edgecache;

public interface EdgeCache {

  boolean invalidate(String key);

}
