package com.hubspot.baragon.models;

public enum RequestAction {
  UPDATE,
  UPDATE_AND_PURGE_CACHE,
  DELETE,
  RELOAD,
  REVERT,
  GET_RENDERED_CONFIG,
  PURGE_CACHE
}
