package org.batfish.representation.rgos;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import static org.batfish.representation.rgos.RgosConversions.convertStaticRoutes;
import static org.batfish.datamodel.Interface.computeInterfaceType;

// import org.batfish.common.BatfishException;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.NavigableSet;
import java.util.TreeSet;


import javax.annotation.Nonnull;
import org.batfish.common.VendorConversionException;

import org.batfish.datamodel.vendor_family.rgos.RgosFamily;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.SubRange;

// import static org.batfish.datamodel.Interface.computeInterfaceType;
import org.batfish.datamodel.InterfaceType;


// import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.IntegerSpace;
// import static org.batfish.datamodel.routing_policy.Common.matchDefaultRoute;

import org.batfish.vendor.VendorConfiguration;
import com.google.common.collect.ImmutableSortedSet;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.InterfaceAddress;

import com.google.common.primitives.Ints;
import com.google.common.collect.ImmutableSet;

// import org.batfish.datamodel.routing_policy.statement.Statement;
// import org.batfish.datamodel.routing_policy.statement.Statements;

// import org.batfish.datamodel.routing_policy.statement.If;


/** Vendor-specific data model for example Cool NOS configuration. */
public final class RgosConfiguration extends VendorConfiguration {
  public static String getCanonicalInterfaceNamePrefix(String prefix) {
    return prefix;
    // for (Entry<String, String> e : CISCO_INTERFACE_PREFIXES.entrySet()) {
    //   String matchPrefix = e.getKey();
    //   String canonicalPrefix = e.getValue();
    //   if (matchPrefix.toLowerCase().startsWith(prefix.toLowerCase())) {
    //     return canonicalPrefix;
    //   }
    // }
    // throw new BatfishException("Invalid interface name prefix: '" + prefix + "'");
  }
  private final Map<String, Vrf> _vrfs;

  public RgosConfiguration() {
    _asPathAccessLists = new TreeMap<>();
    _prefixLists = new TreeMap<>();
    _staticRoutes = new HashMap<>();
    _interfaces = new TreeMap<>();
    _vrfs = new TreeMap<>();
    _rf = new RgosFamily();
    _dhcpRelayServers = new ArrayList<>();
    _dnsServers = new TreeSet<>();

    _vrfs.put(Configuration.DEFAULT_VRF_NAME, new Vrf(Configuration.DEFAULT_VRF_NAME));

  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    Configuration c =
      Configuration.builder()
      .setHostname(_hostname)
      .setConfigurationFormat(_vendor)
      .setDefaultCrossZoneAction(LineAction.PERMIT)
      .setDefaultInboundAction(LineAction.PERMIT)
      .build();

    c.getVendorFamily().setRgos(_rf);
    c.setDefaultInboundAction(LineAction.PERMIT);
    c.setDefaultCrossZoneAction(LineAction.PERMIT);
    c.setDnsServers(_dnsServers);
    // c.setDnsSourceInterface(_dnsSourceInterface);
    // c.setDomainName(_domainName);
    // c.setExportBgpFromBgpRib(true);
    c.setNormalVlanRange(
        IntegerSpace.of(new SubRange(VLAN_NORMAL_MIN_RGOS, VLAN_NORMAL_MAX_RGOS)));
    // c.setTacacsServers(toTacacsServers(_tacacsServers, _aaaServerGroups));
    // c.setTacacsSourceInterface(_tacacsSourceInterface);
    // c.setNtpSourceInterface(_ntpSourceInterface);
    // if (_rf.getNtp() != null) {
    //   c.setNtpServers(new TreeSet<>(_rf.getNtp().getServers().keySet()));
    // }
    // if (_rf.getLogging() != null) {
    //   c.setLoggingSourceInterface(_rf.getLogging().getSourceInterface());
    //   c.setLoggingServers(new TreeSet<>(_rf.getLogging().getHosts().keySet()));
    // }
    // c.setSnmpSourceInterface(_snmpSourceInterface);

    // for (Line line : _rf.getLines().values()) {
    //   String list = line.getLoginAuthentication();
    //   if (list == null) {
    //     continue;
    //   }
    //   boolean found = false;
    //   Aaa aaa = _rf.getAaa();
    //   if (aaa != null) {
    //     AaaAuthentication authentication = aaa.getAuthentication();
    //     if (authentication != null) {
    //       AaaAuthenticationLogin login = authentication.getLogin();
    //       if (login != null && login.getLists().containsKey(list)) {
    //         found = true;
    //       }
    //     }
    //   }
    //   if (!found) {
    //     line.setLoginAuthentication(null);
    //   }
    // }

    // RoutingPolicy.builder()
    //     .setOwner(c)
    //     .setName(RESOLUTION_POLICY_NAME)
    //     .setStatements(
    //         ImmutableList.of(
    //             new If(
    //                 matchDefaultRoute(),
    //                 ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
    //                 ImmutableList.of(Statements.ReturnTrue.toStaticStatement()))))
    //     .build();

    // initialize vrfs
    for (String vrfName : _vrfs.keySet()) {
      c.getVrfs()
          .put(
              vrfName,
              org.batfish.datamodel.Vrf.builder()
                  .setName(vrfName)
                  .setResolutionPolicy(RESOLUTION_POLICY_NAME)
                  .build());
      // inherit address family props
      Vrf vrf = _vrfs.get(vrfName);
      VrfAddressFamily ip4uaf = vrf.getIpv4UnicastAddressFamily();
      if (ip4uaf == null) {
        continue;
      }
      ip4uaf.inherit(vrf.getGenericAddressFamilyConfig());
    }

    convertStaticRoutes(this, c);

    _interfaces.forEach(
        (ifaceName, iface) -> {
          // Handle renaming interfaces for ASA devices
          String newIfaceName = iface.getName();
          org.batfish.datamodel.Interface newInterface = toInterface(newIfaceName, iface, c);
          // String vrfName = iface.getVrf();
          // if (vrfName == null) {
          //   throw new BatfishException("Missing vrf name for iface: '" + iface.getName() + "'");
          // }
          // c.getAllInterfaces().put(newIfaceName, newInterface);
        });
    return c;
  }

  public NavigableSet<String> getDnsServers() {
    return _dnsServers;
  }

  public Map<String, Interface> getInterfaces() {
    return _interfaces;
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

  public boolean getSpanningTreePortfastDefault() {
    return _spanningTreePortfastDefault;
  }

  public void setSpanningTreePortfastDefault(boolean spanningTreePortfastDefault) {
    _spanningTreePortfastDefault = spanningTreePortfastDefault;
  }


  private org.batfish.datamodel.Interface toInterface(
      String ifaceName, Interface iface, Configuration c) {
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(ifaceName)
            .setOwner(c)
            .setType(computeInterfaceType(iface.getName(), c.getConfigurationFormat()))
            .build();
    String vrfName = iface.getVrf();
    Vrf vrf = _vrfs.computeIfAbsent(vrfName, Vrf::new);
    newIface.setDescription(iface.getDescription());
    if (!iface.getActive()) {
      newIface.adminDown();
    }
    // String channelGroup = iface.getChannelGroup();
    // newIface.setChannelGroup(channelGroup);
    // if (iface.getActive() && channelGroup != null && !_interfaces.containsKey(channelGroup)) {
    //   _w.redFlag(
    //       String.format(
    //           "Deactivating interface %s that refers to undefined channel-group %s",
    //           ifaceName, channelGroup));
    //   newIface.deactivate(InactiveReason.INVALID);
    // }

    // newIface.setCryptoMap(iface.getCryptoMap());
    newIface.setVrf(c.getVrfs().get(vrfName));
    newIface.setSpeed(
        firstNonNull(
            iface.getSpeed(),
            Interface.getDefaultSpeed(iface.getName(), c.getConfigurationFormat())));
    newIface.setBandwidth(
        firstNonNull(
            iface.getBandwidth(),
            newIface.getSpeed(),
            Interface.getDefaultBandwidth(iface.getName(), c.getConfigurationFormat())));
    if (iface.getDhcpRelayClient()) {
      newIface.setDhcpRelayAddresses(_dhcpRelayServers);
    } else {
      newIface.setDhcpRelayAddresses(ImmutableList.copyOf(iface.getDhcpRelayAddresses()));
    }
    newIface.setMlagId(iface.getMlagId());
    newIface.setMtu(iface.getMtu());
    newIface.setProxyArp(iface.getProxyArp());
    newIface.setDeclaredNames(ImmutableSortedSet.copyOf(iface.getDeclaredNames()));
    newIface.setSwitchport(iface.getSwitchport());

    if (newIface.getSwitchport()) {
      newIface.setSwitchportMode(iface.getSwitchportMode());

      // switch settings
      if (iface.getSwitchportMode() == SwitchportMode.ACCESS) {
        newIface.setAccessVlan(firstNonNull(iface.getAccessVlan(), 1));
      }

      if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
        SwitchportEncapsulationType encapsulation =
            firstNonNull(
                // TODO: check if this is OK
                iface.getSwitchportTrunkEncapsulation(), SwitchportEncapsulationType.DOT1Q);
        newIface.setSwitchportTrunkEncapsulation(encapsulation);
        if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
          /*
           * Compute allowed VLANs:
           * - If allowed VLANs are set, honor them;
           */
          if (iface.getAllowedVlans() != null) {
            newIface.setAllowedVlans(iface.getAllowedVlans());
          } else {
            newIface.setAllowedVlans(Interface.ALL_VLANS);
          }
        }
        newIface.setNativeVlan(firstNonNull(iface.getNativeVlan(), 1));
      }

      newIface.setSpanningTreePortfast(iface.getSpanningTreePortfast());
    } else {
      newIface.setSwitchportMode(SwitchportMode.NONE);
      if (newIface.getInterfaceType() == InterfaceType.VLAN) {
        Integer vlan = Ints.tryParse(ifaceName.substring("vlan".length()));
        newIface.setVlan(vlan);
        if (vlan == null) {
          _w.redFlag("Unable assign vlan for interface " + ifaceName);
        }
        newIface.setAutoState(iface.getAutoState());
      }

      // All prefixes is the combination of the interface prefix + any secondary prefixes.
      ImmutableSet.Builder<InterfaceAddress> allPrefixes = ImmutableSet.builder();
      if (iface.getAddress() != null) {
        newIface.setAddress(iface.getAddress());
        allPrefixes.add(iface.getAddress());
      }
      allPrefixes.addAll(iface.getSecondaryAddresses());
      newIface.setAllAddresses(allPrefixes.build());

      // subinterface settings
      newIface.setEncapsulationVlan(iface.getEncapsulationVlan());
    }

    // EigrpProcess eigrpProcess = null;
    // if (iface.getAddress() != null) {
    //   for (EigrpProcess process : vrf.getEigrpProcesses().values()) {
    //     if (process.getNetworks().contains(iface.getAddress().getPrefix())) {
    //       // Found a process on interface
    //       if (eigrpProcess != null) {
    //         // Cisco does not recommend running multiple EIGRP autonomous systems on the same
    //         // interface
    //         _w.redFlag("Interface: '" + iface.getName() + "' matches multiple EIGRP processes");
    //         break;
    //       }
    //       eigrpProcess = process;
    //     }
    //   }
    // }
    // // Let toEigrpProcess handle null asn failure
    // if (eigrpProcess != null && eigrpProcess.getAsn() != null) {
    //   boolean passive =
    //       eigrpProcess
    //           .getInterfacePassiveStatus()
    //           .getOrDefault(iface.getName(), eigrpProcess.getPassiveInterfaceDefault());

    //   // Export distribute lists
    //   String exportPolicyName =
    //       eigrpNeighborExportPolicyName(ifaceName, vrfName, eigrpProcess.getAsn());
    //   RoutingPolicy exportPolicy =
    //       generateEigrpPolicy(
    //           c,
    //           this,
    //           Arrays.asList(
    //               eigrpProcess.getOutboundGlobalDistributeList(),
    //               eigrpProcess.getOutboundInterfaceDistributeLists().get(ifaceName)),
    //           ImmutableList.of(matchOwnAsn(eigrpProcess.getAsn())),
    //           exportPolicyName);
    //   c.getRoutingPolicies().put(exportPolicyName, exportPolicy);

    //   // Import distribute lists
    //   String importPolicyName =
    //       eigrpNeighborImportPolicyName(ifaceName, vrfName, eigrpProcess.getAsn());
    //   RoutingPolicy importPolicy =
    //       generateEigrpPolicy(
    //           c,
    //           this,
    //           Arrays.asList(
    //               eigrpProcess.getInboundGlobalDistributeList(),
    //               eigrpProcess.getInboundInterfaceDistributeLists().get(ifaceName)),
    //           ImmutableList.of(),
    //           importPolicyName);
    //   c.getRoutingPolicies().put(importPolicyName, importPolicy);

    //   newIface.setEigrp(
    //       EigrpInterfaceSettings.builder()
    //           .setAsn(eigrpProcess.getAsn())
    //           .setEnabled(true)
    //           .setExportPolicy(exportPolicyName)
    //           .setImportPolicy(importPolicyName)
    //           .setMetric(computeEigrpMetricForInterface(iface, eigrpProcess.getMode()))
    //           .setPassive(passive)
    //           .build());
    //   if (newIface.getEigrp() == null) {
    //     _w.redFlag("Interface: '" + iface.getName() + "' failed to set EIGRP settings");
    //   }
    // }

    // boolean level1 = false;
    // boolean level2 = false;
    // IsisProcess isisProcess = vrf.getIsisProcess();
    // if (isisProcess != null && iface.getIsisInterfaceMode() != IsisInterfaceMode.UNSET) {
    //   switch (isisProcess.getLevel()) {
    //     case LEVEL_1:
    //       level1 = true;
    //       break;
    //     case LEVEL_1_2:
    //       level1 = true;
    //       level2 = true;
    //       break;
    //     case LEVEL_2:
    //       level2 = true;
    //       break;
    //     default:
    //       throw new VendorConversionException("Invalid IS-IS level");
    //   }
    //   IsisInterfaceSettings.Builder isisInterfaceSettingsBuilder = IsisInterfaceSettings.builder();
    //   IsisInterfaceLevelSettings levelSettings =
    //       IsisInterfaceLevelSettings.builder()
    //           .setCost(iface.getIsisCost())
    //           .setMode(iface.getIsisInterfaceMode())
    //           .build();
    //   if (level1) {
    //     isisInterfaceSettingsBuilder.setLevel1(levelSettings);
    //   }
    //   if (level2) {
    //     isisInterfaceSettingsBuilder.setLevel2(levelSettings);
    //   }
    //   newIface.setIsis(isisInterfaceSettingsBuilder.build());
    // }

    // String incomingFilterName = iface.getIncomingFilter();
    // if (incomingFilterName != null) {
    //   newIface.setIncomingFilter(c.getIpAccessLists().get(incomingFilterName));
    // }
    // String outgoingFilterName = iface.getOutgoingFilter();
    // if (outgoingFilterName != null) {
    //   newIface.setOutgoingFilter(c.getIpAccessLists().get(outgoingFilterName));
    // }
    // // Apply zone outgoing filter if necessary
    // applyZoneFilter(iface, newIface, c);

    // /*
    //  * NAT rules are specified at the top level, but are applied as incoming transformations on the
    //  * outside interface (outside-to-inside) and outgoing transformations on the outside interface
    //  * (inside-to-outside)
    //  *
    //  * Currently, only static NATs have both incoming and outgoing transformations
    //  */

    // List<CiscoIosNat> ciscoIosNats = firstNonNull(_ciscoIosNats, ImmutableList.of());
    // if (!ciscoIosNats.isEmpty()) {
    //   generateCiscoIosNatTransformations(ifaceName, vrfName, newIface, c);
    // }

    // String routingPolicyName = iface.getRoutingPolicy();
    // if (routingPolicyName != null) {
    //   newIface.setPacketPolicy(routingPolicyName);
    // }

    // For IOS, FirewallSessionInterfaceInfo is created once for all NAT interfaces.
    return newIface;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _vendor = format;
  }

  public ConfigurationFormat getVendor() {
    return _vendor;
  }

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

  public Map<String, Vrf> getVrfs() {
    return _vrfs;
  }
  public Vrf getDefaultVrf() {
    return _vrfs.get(Configuration.DEFAULT_VRF_NAME);
  }

  public RgosFamily getRf() {
    return _rf;
  }

  public List<Ip> getDhcpRelayServers() {
    return _dhcpRelayServers;
  }

  public static final String DEFAULT_VRF_NAME = "default";

  public static final String MANAGEMENT_VRF_NAME = "management";

  public static final String MANAGEMENT_INTERFACE_PREFIX = "mgmt";
  public static final String RESOLUTION_POLICY_NAME = "~RESOLUTION_POLICY~";

  private static final int VLAN_NORMAL_MAX_RGOS = 4096;

  private static final int VLAN_NORMAL_MIN_RGOS = 2;

  private final Map<String, Interface> _interfaces;
  private final RgosFamily _rf;
  private final List<Ip> _dhcpRelayServers;
  private boolean _spanningTreePortfastDefault;

  // Note: For simplicity, in Cool NOS, you can only have one static route per prefix.
  private @Nonnull Map<Prefix, StaticRoute> _staticRoutes;
  private String _hostname;
  private String _version;
  private NavigableSet<String> _dnsServers;

  private ConfigurationFormat _vendor;

  private final Map<String, IpAsPathAccessList> _asPathAccessLists;
  private final Map<String, PrefixList> _prefixLists;
}
