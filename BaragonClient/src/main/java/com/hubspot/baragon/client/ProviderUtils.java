package com.hubspot.baragon.client;

import javax.inject.Provider;

public class ProviderUtils {
  public static <T> Provider<T> of(final T instance) {
    return new Provider<T>() {
      @Override
      public T get()
      {
        return instance;
      }
    };
  }
}
