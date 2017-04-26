package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAuthKey {
  private final String value;
  private final String owner;
  private final long createdAt;
  private final Optional<Long> expiredAt;

  public static BaragonAuthKey expire(BaragonAuthKey authKey) {
    return new BaragonAuthKey(authKey.getValue(), authKey.getOwner(), authKey.getCreatedAt(), Optional.of(System.currentTimeMillis()));
  }

  @JsonCreator
  public BaragonAuthKey(@JsonProperty("value") String value,
                        @JsonProperty("owner") String owner,
                        @JsonProperty("createdAt") long createdAt,
                        @JsonProperty("expiredAt") Optional<Long> expiredAt) {
    this.value = value;
    this.owner = owner;
    this.createdAt = createdAt;
    this.expiredAt = expiredAt;
  }

  public String getValue() {
    return value;
  }

  public String getOwner() {
    return owner;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public Optional<Long> getExpiredAt() {
    return expiredAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonAuthKey that = (BaragonAuthKey) o;

    if (createdAt != that.createdAt) {
      return false;
    }
    if (!expiredAt.equals(that.expiredAt)) {
      return false;
    }
    if (!owner.equals(that.owner)) {
      return false;
    }
    if (!value.equals(that.value)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + owner.hashCode();
    result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
    result = 31 * result + expiredAt.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("value", value)
        .add("owner", owner)
        .add("createdAt", createdAt)
        .add("expiredAt", expiredAt)
        .toString();
  }
}
