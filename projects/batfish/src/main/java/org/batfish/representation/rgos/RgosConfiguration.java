package org.batfish.representation.rgos;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.datamodel.Interface.computeInterfaceType;
import static org.batfish.datamodel.routing_policy.Common.matchDefaultRoute;
import static org.batfish.datamodel.routing_policy.Common.suppressSummarizedPrefixes;
import static org.batfish.datamodel.routing_policy.Common.initDenyAllBgpRedistributionPolicy;
import static org.batfish.datamodel.bgp.LocalOriginationTypeTieBreaker.NO_PREFERENCE;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.Collection;
import java.util.SortedSet;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.batfish.common.BatfishException;

import javax.annotation.Nullable;

import static org.batfish.representation.rgos.OspfProcess.DEFAULT_LOOPBACK_OSPF_COST;

import static org.batfish.representation.rgos.RgosConversions.toStaticRoute;
import static org.batfish.representation.rgos.RgosConversions.toBgpAggregate;
import static org.batfish.representation.rgos.RgosConversions.generateBgpImportPolicy;
import static org.batfish.representation.rgos.RgosConversions.generateBgpExportPolicy;

import static org.batfish.representation.rgos.RgosConversions.computeDistributeListPolicies;
import static org.batfish.representation.rgos.RgosConversions.toOspfHelloInterval;
import static org.batfish.representation.rgos.RgosConversions.toOspfDeadInterval;
import static org.batfish.representation.rgos.RgosConversions.toOspfNetworkType;

import static org.batfish.representation.rgos.OspfProcess.DEFAULT_LOOPBACK_OSPF_COST;
import static org.batfish.representation.rgos.OspfProcess.DEFAULT_LOOPBACK_OSPF_COST;
import static org.batfish.datamodel.bgp.AllowRemoteAsOutMode.ALWAYS;
import static org.batfish.datamodel.bgp.NextHopIpTieBreaker.HIGHEST_NEXT_HOP_IP;

import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.statement.SetWeight;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;

import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.RouteIsClassful;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;

import javax.annotation.Nonnull;
import org.batfish.common.VendorConversionException;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfNetworkType;

import org.batfish.datamodel.vendor_family.rgos.RgosFamily;

import org.batfish.datamodel.InterfaceType;

import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAcl;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunityAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunityAcl;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAll;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunityIn;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;

import org.batfish.datamodel.routing_policy.statement.Statement;

import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;

import static org.batfish.datamodel.Names.generatedOspfExportPolicyName;
import static org.batfish.datamodel.Names.generatedOspfDefaultRouteGenerationPolicyName;
import static org.batfish.datamodel.Names.generatedBgpRedistributionPolicyName;

import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.SetMetric;

import org.batfish.datamodel.routing_policy.expr.LiteralLong;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.Names;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.BgpActivePeerConfig;

import org.batfish.datamodel.Ip;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.GeneratedRoute;

import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.BgpConfederation;

import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.BgpPeerConfig;

import java.util.stream.Collectors;
import java.util.LinkedHashSet;

import org.batfish.vendor.VendorConfiguration;
import com.google.common.collect.Iterables;

import com.google.common.primitives.Ints;
import com.google.common.collect.ImmutableSet;

import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;

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
    _standardCommunityLists = new TreeMap<>();
    _expandedCommunityLists = new TreeMap<>();
    _tracks = new TreeMap<>();
    _routeMaps = new TreeMap<>();

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

    RoutingPolicy.builder()
        .setOwner(c)
        .setName(RESOLUTION_POLICY_NAME)
        .setStatements(
            ImmutableList.of(
                new If(
                    matchDefaultRoute(),
                    ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement()))))
        .build();

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
    // // snmp server
    // if (_snmpServer != null) {
    //   String snmpServerVrf = _snmpServer.getVrf();
    //   c.getVrfs().get(snmpServerVrf).setSnmpServer(_snmpServer);
    // }

    // convert as path access lists to vendor independent format
    for (IpAsPathAccessList pathList : _asPathAccessLists.values()) {
      AsPathAccessList apList = RgosConversions.toAsPathAccessList(pathList);
      c.getAsPathAccessLists().put(apList.getName(), apList);
    }
    convertIpCommunityLists(c);
    // for (RouteMap map : _routeMaps.values()) {
    //   // convert route maps to RoutingPolicy objects
    //   RoutingPolicy newPolicy = toRoutingPolicy(c, map);
    //   c.getRoutingPolicies().put(newPolicy.getName(), newPolicy);
    // }

    _interfaces.forEach(
        (ifaceName, iface) -> {
          // Handle renaming interfaces for ASA devices
          String newIfaceName = iface.getName();
          org.batfish.datamodel.Interface newInterface = toInterface(newIfaceName, iface, c);
          String vrfName = iface.getVrf();
          if (vrfName == null) {
            throw new BatfishException("Missing vrf name for iface: '" + iface.getName() + "'");
          }
          c.getAllInterfaces().put(newIfaceName, newInterface);
        });

    _vrfs.forEach(
        (vrfName, vrf) -> {
          org.batfish.datamodel.Vrf newVrf = c.getVrfs().get(vrfName);

          // description
          newVrf.setDescription(vrf.getDescription());

          // add snmp trap servers to main list
          if (newVrf.getSnmpServer() != null) {
            c.getSnmpTrapServers().addAll(newVrf.getSnmpServer().getHosts().keySet());
          }

          // convert static routes
          for (StaticRoute staticRoute : vrf.getStaticRoutes()) {
            newVrf.getStaticRoutes().add(toStaticRoute(staticRoute, _tracks::containsKey));
          }
          // For the default VRF, also convert static routes created by add-route in NAT rules
          // if (vrf == getDefaultVrf()) {
          //   newVrf.getStaticRoutes().addAll(generateIosNatAddRouteRoutes());
          // }

          // convert rip process
          // RipProcess ripProcess = vrf.getRipProcess();
          // if (ripProcess != null) {
          //   org.batfish.datamodel.RipProcess newRipProcess = toRipProcess(ripProcess, vrfName, c);
          //   newVrf.setRipProcess(newRipProcess);
          // }

          // Convert OSPF processes.
          newVrf.setOspfProcesses(
              vrf.getOspfProcesses().values().stream()
                  .map(proc -> toOspfProcess(proc, vrfName, c, this))
                  .filter(Objects::nonNull));

          // convert eigrp processes
          // vrf.getEigrpProcesses().values().stream()
          //     .map(proc -> CiscoConversions.toEigrpProcess(proc, vrfName, c, this))
          //     .filter(Objects::nonNull)
          //     .forEach(newVrf::addEigrpProcess);

          // convert isis process
          // IsisProcess isisProcess = vrf.getIsisProcess();
          // if (isisProcess != null) {
          //   org.batfish.datamodel.isis.IsisProcess newIsisProcess =
          //       CiscoConversions.toIsisProcess(isisProcess, c, this);
          //   newVrf.setIsisProcess(newIsisProcess);
          // }

          // convert bgp process
          BgpProcess bgpProcess = vrf.getBgpProcess();
          if (bgpProcess != null) {
            org.batfish.datamodel.BgpProcess newBgpProcess = toBgpProcess(c, bgpProcess, vrfName);
            newVrf.setBgpProcess(newBgpProcess);
          } else if (vrf.getIpv4UnicastAddressFamily() != null
              && !vrf.getIpv4UnicastAddressFamily().getRouteTargetImport().isEmpty()) {
            /*
             * Despite no BGP config this vrf is leaked into. Make a dummy BGP process.
             */
            assert newVrf.getBgpProcess() == null;
            newVrf.setBgpProcess(
                bgpProcessBuilder()
                    .setRouterId(Ip.ZERO)
                    .setRedistributionPolicy(initDenyAllBgpRedistributionPolicy(c))
                    .build());
          }
        });

    return c;
  }

  public NavigableSet<String> getDnsServers() {
    return _dnsServers;
  }

  public Map<Integer, Track> getTracks() {
    return _tracks;
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

  public Map<String, StandardCommunityList> getStandardCommunityLists() {
    return _standardCommunityLists;
  }

  public Map<String, ExpandedCommunityList> getExpandedCommunityLists() {
    return _expandedCommunityLists;
  }


  private void convertIpCommunityLists(Configuration c) {
    // create CommunitySetMatchExpr for route-map match community
    _standardCommunityLists.forEach(
        (name, ipCommunityListStandard) ->
            c.getCommunitySetMatchExprs()
                .put(name, toCommunitySetMatchExpr(ipCommunityListStandard)));
    _expandedCommunityLists.forEach(
        (name, ipCommunityListExpanded) ->
            c.getCommunitySetMatchExprs()
                .put(name, toCommunitySetMatchExpr(ipCommunityListExpanded)));

    // create CommunityMatchExpr for route-map set comm-list delete
    _standardCommunityLists.forEach(
        (name, ipCommunityListStandard) ->
            c.getCommunityMatchExprs().put(name, toCommunityMatchExpr(ipCommunityListStandard)));
    _expandedCommunityLists.forEach(
        (name, ipCommunityListExpanded) ->
            c.getCommunityMatchExprs().put(name, toCommunityMatchExpr(ipCommunityListExpanded)));
  }


  private static @Nonnull CommunityAclLine toCommunityAclLine(ExpandedCommunityListLine line) {
    return new CommunityAclLine(
                                line.getAction(), RgosConversions.toCommunityMatchRegex(line.getRegex()));
  }

  private static @Nonnull CommunitySetAclLine toCommunitySetAclLine(
      StandardCommunityListLine line) {
    return new CommunitySetAclLine(
        line.getAction(),
        CommunitySetMatchAll.matchAll(
            line.getCommunities().stream()
                .map(community -> new HasCommunity(new CommunityIs(community)))
                .collect(ImmutableSet.toImmutableSet())));
  }

  private static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      ExpandedCommunityList ipCommunityListExpanded) {
    return CommunityAcl.acl(
        ipCommunityListExpanded.getLines().stream()
            .map(RgosConfiguration::toCommunityAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      StandardCommunityList ipCommunityListStandard) {
    Set<Community> whitelist = new HashSet<>();
    Set<Community> blacklist = new HashSet<>();
    for (StandardCommunityListLine line : ipCommunityListStandard.getLines()) {
      if (line.getCommunities().size() != 1) {
        continue;
      }
      Community community = Iterables.getOnlyElement(line.getCommunities());
      if (line.getAction() == LineAction.PERMIT) {
        if (!blacklist.contains(community)) {
          whitelist.add(community);
        }
      } else {
        // DENY
        if (!whitelist.contains(community)) {
          blacklist.add(community);
        }
      }
    }
    return new CommunityIn(new LiteralCommunitySet(CommunitySet.of(whitelist)));
  }

  private static CommunitySetMatchExpr toCommunitySetMatchExpr(
      ExpandedCommunityList ipCommunityListExpanded) {
    return CommunitySetAcl.acl(
        ipCommunityListExpanded.getLines().stream()
            .map(RgosConversions::toCommunitySetAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static CommunitySetMatchExpr toCommunitySetMatchExpr(
      StandardCommunityList ipCommunityListStandard) {
    return CommunitySetAcl.acl(
        ipCommunityListStandard.getLines().stream()
            .map(RgosConfiguration::toCommunitySetAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private org.batfish.datamodel.ospf.OspfProcess toOspfProcess(
      OspfProcess proc, String vrfName, Configuration c, RgosConfiguration oldConfig) {
    Ip routerId = proc.getRouterId();
    if (routerId == null) {
      routerId = RgosConversions.getHighestIp(oldConfig.getInterfaces());
      if (routerId == Ip.ZERO) {
        _w.redFlag("No candidates for OSPF router-id");
        return null;
      }
    }
    org.batfish.datamodel.ospf.OspfProcess newProcess =
        org.batfish.datamodel.ospf.OspfProcess.builder()
            .setProcessId(proc.getName())
            .setReferenceBandwidth(proc.getReferenceBandwidth())
            .setAdminCosts(
                org.batfish.datamodel.ospf.OspfProcess.computeDefaultAdminCosts(
                    c.getConfigurationFormat()))
            .setSummaryAdminCost(
                RoutingProtocol.OSPF_IA.getSummaryAdministrativeCost(c.getConfigurationFormat()))
            .setRouterId(routerId)
            .build();

    if (proc.getMaxMetricRouterLsa()) {
      newProcess.setMaxMetricTransitLinks(OspfProcess.MAX_METRIC_ROUTER_LSA);
      if (proc.getMaxMetricIncludeStub()) {
        newProcess.setMaxMetricStubNetworks(OspfProcess.MAX_METRIC_ROUTER_LSA);
      }
      newProcess.setMaxMetricExternalNetworks(proc.getMaxMetricExternalLsa());
      newProcess.setMaxMetricSummaryNetworks(proc.getMaxMetricSummaryLsa());
    }

    // establish areas and associated interfaces
    Map<Long, OspfArea.Builder> areas = new HashMap<>();
    Map<Long, ImmutableSortedSet.Builder<String>> areaInterfacesBuilders = new HashMap<>();

    // Set RFC 1583 compatibility
    newProcess.setRfc1583Compatible(proc.getRfc1583Compatible());

    Set<OspfNetwork> ospfNetworks = computeOspfNetworks(proc, _interfaces.values());

    for (Entry<String, org.batfish.datamodel.Interface> e :
        c.getAllInterfaces(vrfName).entrySet()) {
      org.batfish.datamodel.Interface iface = e.getValue();
      /*
       * Filter out interfaces that do not belong to this process, however if the process name is missing,
       * proceed down to inference based on network addresses.
       */
      Interface vsIface = _interfaces.get(iface.getName());
      if (vsIface.getOspfProcess() != null && !vsIface.getOspfProcess().equals(proc.getName())) {
        continue;
      }
      OspfNetwork network = getOspfNetworkForInterface(vsIface, ospfNetworks);
      if (vsIface.getOspfProcess() == null && network == null) {
        // Interface is not in an OspfNetwork on this process
        continue;
      }

      String ifaceName = e.getKey();
      Long areaNum = vsIface.getOspfArea();
      // OSPF area number was not configured on the interface itself, so get from OspfNetwork
      if (areaNum == null) {
        if (network == null) {
          continue;
        }
        areaNum = network.getArea();
      }
      areas.computeIfAbsent(areaNum, areaNumber -> OspfArea.builder().setNumber(areaNumber));
      ImmutableSortedSet.Builder<String> newAreaInterfacesBuilder =
          areaInterfacesBuilders.computeIfAbsent(areaNum, n -> ImmutableSortedSet.naturalOrder());
      newAreaInterfacesBuilder.add(ifaceName);
      finalizeInterfaceOspfSettings(iface, vsIface, proc, areaNum);
    }
    areaInterfacesBuilders.forEach(
        (areaNum, interfacesBuilder) ->
            areas.get(areaNum).addInterfaces(interfacesBuilder.build()));
    proc.getNssas()
        .forEach(
            (areaId, nssaSettings) -> {
              if (!areas.containsKey(areaId)) {
                return;
              }
              areas.get(areaId).setStubType(StubType.NSSA);
              areas.get(areaId).setNssaSettings(toNssaSettings(nssaSettings));
            });

    proc.getStubs()
        .forEach(
            (areaId, stubSettings) -> {
              if (!areas.containsKey(areaId)) {
                return;
              }
              areas.get(areaId).setStubType(StubType.STUB);
              areas.get(areaId).setStubSettings(toStubSettings(stubSettings));
            });

    // create summarization filters for inter-area routes
    for (Entry<Long, Map<Prefix, OspfAreaSummary>> e1 : proc.getSummaries().entrySet()) {
      long areaLong = e1.getKey();
      Map<Prefix, OspfAreaSummary> summaries = e1.getValue();
      OspfArea.Builder area = areas.get(areaLong);
      String summaryFilterName = "~OSPF_SUMMARY_FILTER:" + vrfName + ":" + areaLong + "~";
      RouteFilterList summaryFilter = new RouteFilterList(summaryFilterName);
      c.getRouteFilterLists().put(summaryFilterName, summaryFilter);
      if (area == null) {
        area = OspfArea.builder().setNumber(areaLong);
        areas.put(areaLong, area);
      }
      area.setSummaryFilter(summaryFilterName);
      for (Entry<Prefix, OspfAreaSummary> e2 : summaries.entrySet()) {
        Prefix prefix = e2.getKey();
        OspfAreaSummary summary = e2.getValue();
        int prefixLength = prefix.getPrefixLength();
        int filterMinPrefixLength =
            summary.isAdvertised()
                ? Math.min(Prefix.MAX_PREFIX_LENGTH, prefixLength + 1)
                : prefixLength;
        summaryFilter.addLine(
            new RouteFilterLine(
                LineAction.DENY,
                IpWildcard.create(prefix),
                new SubRange(filterMinPrefixLength, Prefix.MAX_PREFIX_LENGTH)));
      }
      area.addSummaries(ImmutableSortedMap.copyOf(summaries));
      summaryFilter.addLine(
          new RouteFilterLine(
              LineAction.PERMIT,
              IpWildcard.create(Prefix.ZERO),
              new SubRange(0, Prefix.MAX_PREFIX_LENGTH)));
    }
    newProcess.setAreas(toImmutableSortedMap(areas, Entry::getKey, e -> e.getValue().build()));

    String ospfExportPolicyName = generatedOspfExportPolicyName(vrfName, proc.getName());
    RoutingPolicy ospfExportPolicy = new RoutingPolicy(ospfExportPolicyName, c);
    c.getRoutingPolicies().put(ospfExportPolicyName, ospfExportPolicy);
    List<Statement> ospfExportStatements = ospfExportPolicy.getStatements();
    newProcess.setExportPolicy(ospfExportPolicyName);

    // policy map for default information
    if (proc.getDefaultInformationOriginate()) {
      If ospfExportDefault = new If();
      ospfExportStatements.add(ospfExportDefault);
      ospfExportDefault.setComment("OSPF export default route");
      List<Statement> ospfExportDefaultStatements = ospfExportDefault.getTrueStatements();
      long metric = proc.getDefaultInformationMetric();
      ospfExportDefaultStatements.add(new SetMetric(new LiteralLong(metric)));
      OspfMetricType metricType = proc.getDefaultInformationMetricType();
      ospfExportDefaultStatements.add(new SetOspfMetricType(metricType));
      // add default export map with metric
      String defaultOriginateMapName = proc.getDefaultInformationOriginateMap();
      GeneratedRoute.Builder route =
          GeneratedRoute.builder()
              .setNetwork(Prefix.ZERO)
              .setNonRouting(true)
              .setAdmin(MAX_ADMINISTRATIVE_COST);
      if (defaultOriginateMapName != null) {
        RoutingPolicy ospfDefaultGenerationPolicy =
            c.getRoutingPolicies().get(defaultOriginateMapName);
        if (ospfDefaultGenerationPolicy != null) {
          // TODO This should depend on a default route existing, unless `always` is configured
          // If `always` is configured, maybe the route-map should be ignored. Needs GNS3 check.
          route.setGenerationPolicy(defaultOriginateMapName);
          newProcess.addGeneratedRoute(route.build());
        }
      } else if (proc.getDefaultInformationOriginateAlways()) {
        // add generated aggregate with no precondition
        newProcess.addGeneratedRoute(route.build());
      } else {
        // Use a generated route that will only be generated if a default route exists in RIB
        String defaultRouteGenerationPolicyName =
            generatedOspfDefaultRouteGenerationPolicyName(vrfName, proc.getName());
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(defaultRouteGenerationPolicyName)
            .addStatement(
                new If(
                    matchDefaultRoute(),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement())))
            .build();
        route.setGenerationPolicy(defaultRouteGenerationPolicyName);
        newProcess.addGeneratedRoute(route.build());
      }
      ospfExportDefaultStatements.add(Statements.ExitAccept.toStaticStatement());
      ospfExportDefault.setGuard(
          new Conjunction(
              ImmutableList.of(matchDefaultRoute(), new MatchProtocol(RoutingProtocol.AGGREGATE))));
    }

    computeDistributeListPolicies(proc, newProcess, c, vrfName, proc.getName(), oldConfig, _w);

    // policies for redistributing routes
    ospfExportStatements.addAll(
        proc.getRedistributionPolicies().entrySet().stream()
            .sorted(Entry.comparingByKey())
            .map(e -> convertOspfRedistributionPolicy(e.getValue(), proc))
            .collect(Collectors.toList()));

    return newProcess;
  }

  // For testing.
  If convertOspfRedistributionPolicy(OspfRedistributionPolicy policy, OspfProcess proc) {

    RoutingProtocol protocol = policy.getInstance().getProtocol();
    // All redistribution must match the specified protocol.
    Conjunction ospfExportConditions = new Conjunction();
    if (protocol == RoutingProtocol.EIGRP) {
      ospfExportConditions
          .getConjuncts()
          .add(new MatchProtocol(RoutingProtocol.EIGRP, RoutingProtocol.EIGRP_EX));
    } else if (protocol == RoutingProtocol.ISIS_ANY) {
      ospfExportConditions
          .getConjuncts()
          .add(
              new MatchProtocol(
                  RoutingProtocol.ISIS_EL1,
                  RoutingProtocol.ISIS_EL2,
                  RoutingProtocol.ISIS_L1,
                  RoutingProtocol.ISIS_L2));
    } else {
      ospfExportConditions.getConjuncts().add(new MatchProtocol(protocol));
    }

    // Do not redistribute the default route.
    ospfExportConditions.getConjuncts().add(NOT_DEFAULT_ROUTE);

    ImmutableList.Builder<Statement> ospfExportStatements = ImmutableList.builder();

    // Set the metric type and value.
    ospfExportStatements.add(new SetOspfMetricType(policy.getMetricType()));
    long metric =
        policy.getMetric() != null ? policy.getMetric() : proc.getDefaultMetric(_vendor, protocol);
    ospfExportStatements.add(new SetMetric(new LiteralLong(metric)));

    // If only classful routes should be redistributed, filter to classful routes.
    if (policy.getOnlyClassfulRoutes()) {
      ospfExportConditions.getConjuncts().add(RouteIsClassful.instance());
    }

    // If a route-map filter is present, honor it.
    String exportRouteMapName = policy.getRouteMap();
    if (exportRouteMapName != null) {
      RouteMap exportRouteMap = _routeMaps.get(exportRouteMapName);
      if (exportRouteMap != null) {
        ospfExportConditions.getConjuncts().add(new CallExpr(exportRouteMapName));
      }
    }

    ospfExportStatements.add(Statements.ExitAccept.toStaticStatement());

    // Construct the policy and add it before returning.
    return new If(
        "OSPF export routes for " + protocol.protocolName(),
        ospfExportConditions,
        ospfExportStatements.build(),
        ImmutableList.of());
  }

  private org.batfish.datamodel.BgpProcess toBgpProcess(
      Configuration c, BgpProcess proc, String vrfName) {
    Ip bgpRouterId = getBgpRouterId(c, vrfName, proc);
    org.batfish.datamodel.BgpProcess newBgpProcess =
        // TODO: customizable admin distances
        bgpProcessBuilder().setRouterId(bgpRouterId).build();
    newBgpProcess.setClusterListAsIbgpCost(true);
    // BgpTieBreaker tieBreaker = proc.getTieBreaker();
    // if (tieBreaker != null) {
    //   newBgpProcess.setTieBreaker(tieBreaker);
    // }
    MultipathEquivalentAsPathMatchMode multipathEquivalentAsPathMatchMode =
        proc.getAsPathMultipathRelax() ? PATH_LENGTH : EXACT_PATH;
    newBgpProcess.setMultipathEquivalentAsPathMatchMode(multipathEquivalentAsPathMatchMode);
    boolean multipathEbgp = false;
    boolean multipathIbgp = false;
    if (firstNonNull(proc.getMaximumPaths(), 0) > 1) {
      multipathEbgp = true;
      multipathIbgp = true;
    }
    if (firstNonNull(proc.getMaximumPathsEbgp(), 0) > 1) {
      multipathEbgp = true;
    }
    if (firstNonNull(proc.getMaximumPathsIbgp(), 0) > 1) {
      multipathIbgp = true;
    }
    newBgpProcess.setMultipathEbgp(multipathEbgp);
    newBgpProcess.setMultipathIbgp(multipathIbgp);

    // Global confederation config
    Long confederation = proc.getConfederation();
    if (confederation != null && !proc.getConfederationMembers().isEmpty()) {
      newBgpProcess.setConfederation(
          new BgpConfederation(confederation, proc.getConfederationMembers()));
    }

    // Populate process-level BGP aggregates
    proc.getAggregateNetworks().values().stream()
        .map(ipv4Aggregate -> toBgpAggregate(ipv4Aggregate, c))
        .forEach(newBgpProcess::addAggregate);

    /*
     * Create common BGP export policy. This policy's only function is to prevent export of
     * suppressed routes (contributors to summary-only aggregates).
     */
    RoutingPolicy.Builder bgpCommonExportPolicy =
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(Names.generatedBgpCommonExportPolicyName(vrfName));

    // Never export routes suppressed because they are more specific than summary-only aggregate
    Stream<Prefix> summaryOnlyNetworks =
        proc.getAggregateNetworks().entrySet().stream()
            .filter(e -> e.getValue().getSummaryOnly())
            .map(Entry::getKey);
    If suppressSummaryOnly = suppressSummarizedPrefixes(c, vrfName, summaryOnlyNetworks);
    if (suppressSummaryOnly != null) {
      bgpCommonExportPolicy.addStatement(suppressSummaryOnly);
    }

    // Finalize common export policy
    bgpCommonExportPolicy.addStatement(Statements.ReturnTrue.toStaticStatement()).build();

    // Create BGP redistribution policy to import main RIB routes into BGP RIB
    String redistPolicyName = generatedBgpRedistributionPolicyName(vrfName);
    RoutingPolicy.Builder redistributionPolicy =
        RoutingPolicy.builder().setOwner(c).setName(redistPolicyName);

    // For IOS, local routes have a default weight of 32768.
    redistributionPolicy.addStatement(new SetWeight(new LiteralInt(DEFAULT_LOCAL_BGP_WEIGHT)));

    // Redistribute routes
    Stream.of(
            RoutingProtocol.RIP,
            RoutingProtocol.STATIC,
            RoutingProtocol.CONNECTED,
            RoutingProtocol.OSPF,
            RoutingProtocol.EIGRP)
        .forEach(
            redistProtocol -> matchRedistributedRoutes(proc, redistProtocol, redistributionPolicy));

    // cause ip peer groups to inherit unset fields from owning named peer
    // group if it exists, and then always from process master peer group
    Set<LeafBgpPeerGroup> leafGroups = new LinkedHashSet<>();
    leafGroups.addAll(proc.getIpPeerGroups().values());
    leafGroups.addAll(proc.getIpv6PeerGroups().values());
    leafGroups.addAll(proc.getDynamicIpPeerGroups().values());
    leafGroups.addAll(proc.getDynamicIpv6PeerGroups().values());
    for (LeafBgpPeerGroup lpg : leafGroups) {
      lpg.inheritUnsetFields(proc, this);
    }

    // create origination prefilter from listed advertised networks
    proc.getIpNetworks()
        .forEach(
            (prefix, bgpNetwork) -> {
              Conjunction exportNetworkConditions = new Conjunction();
              PrefixSpace space = new PrefixSpace();
              space.addPrefix(prefix);
              newBgpProcess.addToOriginationSpace(space);
              exportNetworkConditions
                  .getConjuncts()
                  .add(
                      new MatchPrefixSet(
                          DestinationNetwork.instance(), new ExplicitPrefixSet(space)));
              exportNetworkConditions
                  .getConjuncts()
                  .add(
                      new Not(
                          new MatchProtocol(
                              RoutingProtocol.BGP,
                              RoutingProtocol.IBGP,
                              RoutingProtocol.AGGREGATE)));
              Optional.ofNullable(bgpNetwork.getRouteMapName())
                  .map(_routeMaps::get)
                  .ifPresent(
                      rm -> exportNetworkConditions.getConjuncts().add(new CallExpr(rm.getName())));
              redistributionPolicy.addStatement(
                  new If(
                      "Add network statement routes to BGP",
                      exportNetworkConditions,
                      ImmutableList.of(
                          new SetOrigin(new LiteralOrigin(OriginType.IGP, null)),
                          Statements.ExitAccept.toStaticStatement())));
            });

    // Finalize redistribution policy and attach to process
    redistributionPolicy.addStatement(Statements.ExitReject.toStaticStatement()).build();
    newBgpProcess.setRedistributionPolicy(redistPolicyName);

    for (LeafBgpPeerGroup lpg : leafGroups) {
      if (!lpg.getActive() || lpg.getShutdown()) {
        continue;
      }
      if (lpg.getRemoteAs() == null) {
        _w.redFlag("No remote-as set for peer: " + lpg.getName());
        continue;
      }
      if (lpg instanceof Ipv6BgpPeerGroup
          || lpg instanceof DynamicIpv6BgpPeerGroup
          || lpg.getNeighborPrefix6() != null) {
        // TODO: implement ipv6 bgp neighbors
        continue;
      }
      // update source
      String updateSourceInterface = lpg.getUpdateSource();
      assert lpg.getNeighborPrefix() != null;
      Ip updateSource = getUpdateSource(c, vrfName, lpg, updateSourceInterface);

      // Get default-originate generation policy
      String defaultOriginateGenerationMap = null;
      if (lpg.getDefaultOriginate()) {
        defaultOriginateGenerationMap = lpg.getDefaultOriginateMap();
      }

      // Generate import and export policies
      String peerImportPolicyName = generateBgpImportPolicy(lpg, vrfName, c, _w);
      generateBgpExportPolicy(lpg, vrfName, c, _w);

      // If defaultOriginate is set, create default route for this peer group
      GeneratedRoute.Builder defaultRoute = null;
      if (lpg.getDefaultOriginate()) {
        defaultRoute = GeneratedRoute.builder();
        defaultRoute.setNetwork(Prefix.ZERO);
        defaultRoute.setAdmin(MAX_ADMINISTRATIVE_COST);

        if (defaultOriginateGenerationMap != null
            && c.getRoutingPolicies().containsKey(defaultOriginateGenerationMap)) {
          // originate contingent on generation policy
          defaultRoute.setGenerationPolicy(defaultOriginateGenerationMap);
        }
      }

      Ip clusterId = lpg.getClusterId();
      if (clusterId == null) {
        clusterId = bgpRouterId;
      }
      String description = lpg.getDescription();
      Long pgLocalAs = lpg.getLocalAs();
      long localAs = pgLocalAs != null ? pgLocalAs : proc.getProcnum();

      BgpPeerConfig.Builder<?, ?> newNeighborBuilder;
      if (lpg instanceof IpBgpPeerGroup) {
        IpBgpPeerGroup ipg = (IpBgpPeerGroup) lpg;
        newNeighborBuilder =
            BgpActivePeerConfig.builder()
                .setPeerAddress(ipg.getIp())
                .setRemoteAsns(
                    Optional.ofNullable(lpg.getRemoteAs())
                        .map(LongSpace::of)
                        .orElse(LongSpace.EMPTY));
      } else if (lpg instanceof DynamicIpBgpPeerGroup) {
        DynamicIpBgpPeerGroup dpg = (DynamicIpBgpPeerGroup) lpg;
        LongSpace.Builder asns = LongSpace.builder().including(dpg.getRemoteAs());
        Optional.ofNullable(dpg.getAlternateAs()).ifPresent(asns::includingAll);
        newNeighborBuilder =
            BgpPassivePeerConfig.builder()
                .setPeerPrefix(dpg.getPrefix())
                .setRemoteAsns(asns.build());
      } else {
        throw new VendorConversionException("Invalid BGP leaf neighbor type");
      }
      newNeighborBuilder.setBgpProcess(newBgpProcess);
      newNeighborBuilder.setConfederation(proc.getConfederation());
      newNeighborBuilder.setEnforceFirstAs(firstNonNull(proc.getEnforceFirstAs(), Boolean.TRUE));

      AddressFamilyCapabilities ipv4AfSettings =
          AddressFamilyCapabilities.builder()
              .setAdditionalPathsReceive(lpg.getAdditionalPathsReceive())
              .setAdditionalPathsSelectAll(lpg.getAdditionalPathsSelectAll())
              .setAdditionalPathsSend(lpg.getAdditionalPathsSend())
              .setAllowLocalAsIn(lpg.getAllowAsIn())
              .setAllowRemoteAsOut(ALWAYS) /* no outgoing remote-as check on IOS */
              /*
               * On Cisco IOS, advertise-inactive is true by default. This can be modified by
               * "bgp suppress-inactive" command,
               * which we currently do not parse/extract. So we choose the default value here.
               */
              .setAdvertiseInactive(true)
              .setSendCommunity(lpg.getSendCommunity())
              .setSendExtendedCommunity(lpg.getSendExtendedCommunity())
              .build();
      newNeighborBuilder.setIpv4UnicastAddressFamily(
          Ipv4UnicastAddressFamily.builder()
              .setAddressFamilyCapabilities(ipv4AfSettings)
              .setImportPolicy(peerImportPolicyName)
              .setExportPolicy(Names.generatedBgpPeerExportPolicyName(vrfName, lpg.getName()))
              .setRouteReflectorClient(lpg.getRouteReflectorClient())
              .build());
      newNeighborBuilder.setClusterId(clusterId.asLong());
      newNeighborBuilder.setDefaultMetric(proc.getDefaultMetric());
      newNeighborBuilder.setDescription(description);
      newNeighborBuilder.setEbgpMultihop(lpg.getEbgpMultihop());
      if (defaultRoute != null) {
        newNeighborBuilder.setGeneratedRoutes(ImmutableSet.of(defaultRoute.build()));
      }
      newNeighborBuilder.setGroup(lpg.getGroupName());
      newNeighborBuilder.setLocalAs(localAs);
      newNeighborBuilder.setLocalIp(updateSource);
      newNeighborBuilder.build();
    }
    return newBgpProcess;
  }

  @Nonnull
  private org.batfish.datamodel.BgpProcess.Builder bgpProcessBuilder() {
    return org.batfish.datamodel.BgpProcess.builder()
        .setEbgpAdminCost(DEFAULT_EBGP_ADMIN)
        .setIbgpAdminCost(DEFAULT_IBGP_ADMIN)
        .setLocalAdminCost(DEFAULT_LOCAL_ADMIN)
        .setLocalOriginationTypeTieBreaker(NO_PREFERENCE)
        .setNetworkNextHopIpTieBreaker(HIGHEST_NEXT_HOP_IP)
        .setRedistributeNextHopIpTieBreaker(HIGHEST_NEXT_HOP_IP);
  }

  private static @Nullable OspfNetwork getOspfNetworkForInterface(
      Interface iface, Set<OspfNetwork> ospfNetworks) {
    ConcreteInterfaceAddress interfaceAddress = iface.getAddress();
    if (interfaceAddress == null) {
      // Iface has no IP address / isn't associated with a network in this OSPF process
      return null;
    }

    // Sort networks with longer prefixes first, then lower start IPs and areas
    SortedSet<OspfNetwork> networks =
        ImmutableSortedSet.copyOf(
            Comparator.<OspfNetwork>comparingInt(n -> n.getPrefix().getPrefixLength())
                .reversed()
                .thenComparing(n -> n.getPrefix().getStartIp())
                .thenComparingLong(OspfNetwork::getArea),
            ospfNetworks);
    for (OspfNetwork network : networks) {
      Prefix networkPrefix = network.getPrefix();
      Ip networkAddress = networkPrefix.getStartIp();
      Ip maskedInterfaceAddress =
          interfaceAddress.getIp().getNetworkAddress(networkPrefix.getPrefixLength());
      if (maskedInterfaceAddress.equals(networkAddress)) {
        // Found a longest prefix match, so found the network in this OSPF process for the iface
        return network;
      }
    }
    return null;
  }

  private @Nullable Statement createRedistributionStatements(
      BgpProcess bgpProcess, RedistributionPolicy redistributionPolicy) {
    String mapName = redistributionPolicy.getRouteMap();
    if (mapName != null && !_routeMaps.containsKey(mapName)) {
      // Route-map is undefined. No redistribution will occur.
      return null;
    }
    MatchProtocol matchProtocol;
    RoutingProtocol srcProtocol = redistributionPolicy.getInstance().getProtocol();
    switch (srcProtocol) {
      case RIP:
      case STATIC:
      case CONNECTED:
        matchProtocol = new MatchProtocol(srcProtocol);
        break;
      case OSPF:
        matchProtocol =
            firstNonNull(
                (MatchProtocol)
                    redistributionPolicy
                        .getSpecialAttributes()
                        .get(BgpRedistributionPolicy.OSPF_ROUTE_TYPES),
                // No match type means internal routes only, at least on IOS.
                // https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/5242-bgp-ospf-redis.html#redistributionofonlyospfinternalroutesintobgp
                new MatchProtocol(
                    RoutingProtocol.OSPF, RoutingProtocol.OSPF_IA, RoutingProtocol.OSPF_IS));
        break;
      case EIGRP:
        // key EIGRP indicates redist external too; EIGRP_EX is never used as a key
        matchProtocol = new MatchProtocol(RoutingProtocol.EIGRP, RoutingProtocol.EIGRP_EX);
        break;
      default:
        throw new IllegalStateException(
            String.format("Unexpected protocol for BGP redistribution: %s", srcProtocol));
    }
    List<BooleanExpr> matchConjuncts =
        Stream.of(
                matchProtocol,
                bgpProcess.getDefaultInformationOriginate() ? null : NOT_DEFAULT_ROUTE,
                mapName == null ? null : new CallExpr(mapName))
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    Conjunction redistExpr = new Conjunction(matchConjuncts);
    redistExpr.setComment(String.format("Redistribute %s routes into BGP", srcProtocol));
    return new If(redistExpr, ImmutableList.of(Statements.ExitAccept.toStaticStatement()));
  }


  private void finalizeInterfaceOspfSettings(
      org.batfish.datamodel.Interface iface,
      Interface vsIface,
      @Nullable OspfProcess proc,
      @Nullable Long areaNum) {
    String ifaceName = vsIface.getName();
    OspfInterfaceSettings.Builder ospfSettings = OspfInterfaceSettings.builder().setPassive(false);
    if (proc != null) {
      ospfSettings.setProcess(proc.getName());
      if (firstNonNull(
          vsIface.getOspfPassive(),
          proc.getPassiveInterfaces().contains(ifaceName)
              || (proc.getPassiveInterfaceDefault()
                  ^ proc.getNonDefaultInterfaces().contains(ifaceName)))) {
        proc.getPassiveInterfaces().add(ifaceName);
        ospfSettings.setPassive(true);
      }
    }
    ospfSettings.setHelloMultiplier(vsIface.getOspfHelloMultiplier());

    ospfSettings.setAreaName(areaNum);
    ospfSettings.setEnabled(proc != null && areaNum != null && !vsIface.getOspfShutdown());
    org.batfish.datamodel.ospf.OspfNetworkType networkType =
        toOspfNetworkType(vsIface.getOspfNetworkType(), _w);
    ospfSettings.setNetworkType(networkType);
    if (vsIface.getOspfCost() == null
        && iface.isLoopback()
        && networkType != OspfNetworkType.POINT_TO_POINT) {
      ospfSettings.setCost(DEFAULT_LOOPBACK_OSPF_COST);
    } else {
      ospfSettings.setCost(vsIface.getOspfCost());
    }
    ospfSettings.setHelloInterval(toOspfHelloInterval(vsIface, networkType));
    ospfSettings.setDeadInterval(toOspfDeadInterval(vsIface, networkType));

    iface.setOspfSettings(ospfSettings.build());
  }

  private org.batfish.datamodel.ospf.NssaSettings toNssaSettings(NssaSettings nssaSettings) {
    return org.batfish.datamodel.ospf.NssaSettings.builder()
        .setDefaultOriginateType(
            nssaSettings.getDefaultInformationOriginate()
                ? OspfDefaultOriginateType.INTER_AREA
                : OspfDefaultOriginateType.NONE)
        .setSuppressType3(nssaSettings.getNoSummary())
        .setSuppressType7(nssaSettings.getNoRedistribution())
        .build();
  }

  private org.batfish.datamodel.ospf.StubSettings toStubSettings(StubSettings stubSettings) {
    return org.batfish.datamodel.ospf.StubSettings.builder()
        .setSuppressType3(stubSettings.getNoSummary())
        .build();
  }

  private static Set<OspfNetwork> computeOspfNetworks(
      OspfProcess proc, Collection<Interface> interfaces) {
    ImmutableSet.Builder<OspfNetwork> networks = ImmutableSet.builder();

    for (Interface i : interfaces) {
      ConcreteInterfaceAddress address = i.getAddress();
      if (address == null) {
        continue;
      }
      for (OspfWildcardNetwork wn : proc.getWildcardNetworks()) {
        // first we check if the interface ip address matches the ospf
        // network when the wildcard is ORed to both
        long wildcardLong = wn.getWildcard().asLong();
        long ospfNetworkLong = wn.getNetworkAddress().asLong();
        long intIpLong = address.getIp().asLong();
        long wildcardedOspfNetworkLong = ospfNetworkLong | wildcardLong;
        long wildcardedIntIpLong = intIpLong | wildcardLong;
        if (wildcardedOspfNetworkLong == wildcardedIntIpLong) {
          // since we have a match, we add the INTERFACE network, ignoring
          // the wildcard stuff from before
          Prefix newOspfNetwork = address.getPrefix();
          networks.add(new OspfNetwork(newOspfNetwork, wn.getArea()));
          break;
        }
      }
    }

    return networks.build();
  }

  private Ip getBgpRouterId(Configuration c, String vrfName, BgpProcess proc) {
    Ip processRouterId = proc.getRouterId();
    if (processRouterId == null) {
      processRouterId = _vrfs.get(Configuration.DEFAULT_VRF_NAME).getBgpProcess().getRouterId();
    }
    if (processRouterId == null) {
      processRouterId = Ip.ZERO;
      for (Entry<String, org.batfish.datamodel.Interface> e :
          c.getAllInterfaces(vrfName).entrySet()) {
        String iname = e.getKey();
        org.batfish.datamodel.Interface iface = e.getValue();
        if (iname.startsWith("Loopback")) {
          ConcreteInterfaceAddress address = iface.getConcreteAddress();
          if (address != null) {
            Ip currentIp = address.getIp();
            if (currentIp.asLong() > processRouterId.asLong()) {
              processRouterId = currentIp;
            }
          }
        }
      }
      if (processRouterId.equals(Ip.ZERO)) {
        for (org.batfish.datamodel.Interface currentInterface :
            c.getAllInterfaces(vrfName).values()) {
          ConcreteInterfaceAddress address = currentInterface.getConcreteAddress();
          if (address != null) {
            Ip currentIp = address.getIp();
            if (currentIp.asLong() > processRouterId.asLong()) {
              processRouterId = currentIp;
            }
          }
        }
      }
    }
    return processRouterId;
  }

  private Ip getUpdateSource(
      Configuration c, String vrfName, LeafBgpPeerGroup lpg, String updateSourceInterface) {
    Ip updateSource = null;
    if (updateSourceInterface != null) {
      org.batfish.datamodel.Interface sourceInterface =
          c.getAllInterfaces(vrfName).get(updateSourceInterface);
      if (sourceInterface != null) {
        ConcreteInterfaceAddress address = sourceInterface.getConcreteAddress();
        if (address != null) {
          Ip sourceIp = address.getIp();
          updateSource = sourceIp;
        } else {
          _w.redFlag(
              "bgp update source interface: '"
                  + updateSourceInterface
                  + "' not assigned an ip address");
        }
      }
    } else {
      if (lpg instanceof DynamicIpBgpPeerGroup) {
        updateSource = Ip.AUTO;
      } else {
        Ip neighborAddress = lpg.getNeighborPrefix().getStartIp();
        for (org.batfish.datamodel.Interface iface : c.getAllInterfaces(vrfName).values()) {
          for (ConcreteInterfaceAddress interfaceAddress : iface.getAllConcreteAddresses()) {
            if (interfaceAddress.getPrefix().containsIp(neighborAddress)) {
              Ip ifaceAddress = interfaceAddress.getIp();
              updateSource = ifaceAddress;
            }
          }
        }
      }
    }
    return updateSource;
  }
  private void matchRedistributedRoutes(
      BgpProcess bgpProcess,
      RoutingProtocol srcProtocol,
      RoutingPolicy.Builder redistributionPolicy) {
    bgpProcess.getRedistributionPolicies().entrySet().stream()
        .filter(entry -> entry.getKey().getProtocol().equals(srcProtocol))
        .sorted(Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .map(policy -> createRedistributionStatements(bgpProcess, policy))
        .filter(Objects::nonNull)
        .forEach(redistributionPolicy::addStatement);
  }
  public static final String DEFAULT_VRF_NAME = "default";
  public static final int DEFAULT_EBGP_ADMIN = 20;
  public static final int DEFAULT_LOCAL_ADMIN = 200;

  public static final String MANAGEMENT_VRF_NAME = "management";
  public static final int DEFAULT_IBGP_ADMIN = 200;

  public static final String MANAGEMENT_INTERFACE_PREFIX = "mgmt";
  public static final String RESOLUTION_POLICY_NAME = "~RESOLUTION_POLICY~";
  public static final int DEFAULT_LOCAL_BGP_WEIGHT = 32768;

  private static final int VLAN_NORMAL_MAX_RGOS = 4096;

  static final int MAX_ADMINISTRATIVE_COST = 32767;

  private static final int VLAN_NORMAL_MIN_RGOS = 2;
  private final Map<String, StandardCommunityList> _standardCommunityLists;
  private final Map<String, ExpandedCommunityList> _expandedCommunityLists;


  private final Map<String, Interface> _interfaces;
  private final RgosFamily _rf;
  private final List<Ip> _dhcpRelayServers;
  private boolean _spanningTreePortfastDefault;
  private final Map<Integer, Track> _tracks;

  // Note: For simplicity, in Cool NOS, you can only have one static route per prefix.
  private @Nonnull Map<Prefix, StaticRoute> _staticRoutes;
  private String _hostname;
  private String _version;
  private NavigableSet<String> _dnsServers;

  private ConfigurationFormat _vendor;
  static final Not NOT_DEFAULT_ROUTE = new Not(matchDefaultRoute());
  private final Map<String, RouteMap> _routeMaps;
  public static String computeBgpPeerImportPolicyName(String vrf, String peer) {
    return String.format("~BGP_PEER_IMPORT_POLICY:%s:%s~", vrf, peer);
  }
  public static String computeBgpDefaultRouteExportPolicyName(String vrf, String peer) {
    return String.format("~BGP_DEFAULT_ROUTE_PEER_EXPORT_POLICY:IPv4:%s:%s~", vrf, peer);
  }

  private final Map<String, IpAsPathAccessList> _asPathAccessLists;
  private final Map<String, PrefixList> _prefixLists;
}
