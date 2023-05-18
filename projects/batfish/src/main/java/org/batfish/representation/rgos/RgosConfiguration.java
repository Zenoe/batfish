package org.batfish.representation.rgos;
import static org.batfish.representation.rgos.RgosConversions.convertStaticRoutes;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import org.batfish.common.VendorConversionException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.vendor.VendorConfiguration;

/** Vendor-specific data model for example Cool NOS configuration. */
public final class RgosConfiguration extends VendorConfiguration {

  public RgosConfiguration() {
    _asPathAccessLists = new TreeMap<>();
    _prefixLists = new TreeMap<>();
    _staticRoutes = new HashMap<>();
  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    Configuration c =
        Configuration.builder()
            .setHostname(_hostname)
            .setDefaultCrossZoneAction(LineAction.PERMIT)
            .setDefaultInboundAction(LineAction.PERMIT)
            .build();

    convertStaticRoutes(this, c);

    return c;
  }

  @Override
  public String getHostname() {
    return _hostname;
  }

  public void setVersion(String version) {
    _version = version;
  }

  @Override
  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  public @Nonnull Map<Prefix, StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {}

  @Override
  public List<Configuration> toVendorIndependentConfigurations() throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  public Map<String, IpAsPathAccessList> getAsPathAccessLists() {
    return _asPathAccessLists;
  }

  public Map<String, PrefixList> getPrefixLists() {
    return _prefixLists;
  }
  // Note: For simplicity, in Cool NOS, you can only have one static route per prefix.
  private @Nonnull Map<Prefix, StaticRoute> _staticRoutes;
  private String _hostname;
  private String _version;

  private final Map<String, IpAsPathAccessList> _asPathAccessLists;
  private final Map<String, PrefixList> _prefixLists;
}
