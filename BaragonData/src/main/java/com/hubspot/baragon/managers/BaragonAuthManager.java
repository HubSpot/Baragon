package com.hubspot.baragon.managers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.models.BaragonAuthKey;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class BaragonAuthManager {
  private final AtomicReference<Map<String, BaragonAuthKey>> authKeys;

  @Inject
  public BaragonAuthManager(
    @Named(
      BaragonDataModule.BARAGON_AUTH_KEY_MAP
    ) AtomicReference<Map<String, BaragonAuthKey>> authKeys
  ) {
    this.authKeys = authKeys;
  }

  public boolean isAuthenticated(String authKey) {
    final Optional<BaragonAuthKey> maybeAuthKey = Optional.fromNullable(
      authKeys.get().get(authKey)
    );

    return (
      maybeAuthKey.isPresent() &&
      (
        !maybeAuthKey.get().getExpiredAt().isPresent() ||
        maybeAuthKey.get().getExpiredAt().get() > System.currentTimeMillis()
      )
    );
  }
}
