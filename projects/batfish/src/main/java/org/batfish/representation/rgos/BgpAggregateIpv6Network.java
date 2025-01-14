package org.batfish.representation.rgos;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Prefix6;

@ParametersAreNonnullByDefault
public class BgpAggregateIpv6Network extends BgpAggregateNetwork {

  public BgpAggregateIpv6Network(Prefix6 prefix6) {
    this(prefix6, false, null, null, null, false);
  }

  public BgpAggregateIpv6Network(
      Prefix6 prefix6,
      boolean asSet,
      @Nullable String suppressMap,
      @Nullable String advertiseMap,
      @Nullable String attributeMap,
      boolean summaryOnly) {
    setAsSet(asSet);
    _prefix6 = prefix6;
    setAdvertiseMap(advertiseMap);
    setSuppressMap(suppressMap);
    setAttributeMap(attributeMap);
    setSummaryOnly(summaryOnly);
  }

  public Prefix6 getPrefix6() {
    return _prefix6;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof BgpAggregateIpv6Network)) {
      return false;
    }
    BgpAggregateIpv6Network rhs = (BgpAggregateIpv6Network) o;
    return baseEquals(rhs) && _prefix6.equals(rhs._prefix6);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseHashcode(), _prefix6);
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .omitNullValues()
        .add("prefix6", _prefix6)
        .add("advertiseMap", getAdvertiseMap())
        .add("asSet", getAsSet())
        .add("attributeMap", getAttributeMap())
        .add("summaryOnly", getSummaryOnly())
        .add("suppressMap", getSuppressMap())
        .toString();
  }

  private final @Nonnull Prefix6 _prefix6;
}
