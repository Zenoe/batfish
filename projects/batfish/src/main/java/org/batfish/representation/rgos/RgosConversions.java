package org.batfish.representation.rgos;
import static org.batfish.datamodel.routing_policy.Common.generateSuppressionPolicy;
import static org.batfish.datamodel.Names.generatedBgpCommonExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerExportPolicyName;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAclLine;
import org.batfish.datamodel.routing_policy.communities.ColonSeparatedRendering;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchRegex;
import org.batfish.datamodel.routing_policy.communities.TypesFirstAscendingSpaceSeparated;
import org.batfish.datamodel.route.nh.NextHop;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;

import com.google.common.annotations.VisibleForTesting;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.Common;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayList;
import org.batfish.datamodel.OriginType;
import static java.util.Collections.singletonList;

import org.batfish.datamodel.ConcreteInterfaceAddress;

import java.util.function.Predicate;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;

import org.batfish.common.Warnings;
import org.batfish.datamodel.bgp.BgpAggregate;

import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.Ip;
import javax.annotation.Nullable;
import static org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.Objects;

import java.util.stream.Collectors;
import org.batfish.representation.rgos.DistributeList.DistributeListFilterType;
import static org.batfish.representation.rgos.RgosConfiguration.computeBgpPeerImportPolicyName;
import static org.batfish.representation.rgos.RgosConfiguration.computeBgpDefaultRouteExportPolicyName;

import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.RoutingProtocol;

import org.batfish.datamodel.routing_policy.statement.Statements;

/**
 * Utility class for converting {@link RgosConfiguration} to vendor-independent {@link
 * Configuration}.
 */
public final class RgosConversions {
  static int DEFAULT_OSPF_HELLO_INTERVAL = 30;

  static int DEFAULT_OSPF_HELLO_INTERVAL_P2P_AND_BROADCAST = 10;
  static int OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER = 4;

  static int DEFAULT_OSPF_DEAD_INTERVAL_P2P_AND_BROADCAST =
      OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * DEFAULT_OSPF_HELLO_INTERVAL_P2P_AND_BROADCAST;

  static int DEFAULT_OSPF_DEAD_INTERVAL =
      OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * DEFAULT_OSPF_HELLO_INTERVAL;


  static AsPathAccessList toAsPathAccessList(IpAsPathAccessList pathList) {
    List<AsPathAccessListLine> lines =
        pathList.getLines().stream()
            .map(IpAsPathAccessListLine::toAsPathAccessListLine)
            .collect(ImmutableList.toImmutableList());
    return new AsPathAccessList(pathList.getName(), lines);
  }


  // static void convertStaticRoutes(RgosConfiguration vc, Configuration c) {
  //   vc.getStaticRoutes()
  //       .forEach(
  //           (prefix, route) ->
  //               c.getDefaultVrf().getStaticRoutes().add(toStaticRoute(prefix, route)));
  // }
  static @Nonnull CommunityMatchRegex toCommunityMatchRegex(String regex) {
    return new CommunityMatchRegex(ColonSeparatedRendering.instance(), toJavaRegex(regex));
  }

  static Ip getHighestIp(Map<String, Interface> allInterfaces) {
    Map<String, Interface> interfacesToCheck;
    Map<String, Interface> loopbackInterfaces = new HashMap<>();
    for (Entry<String, Interface> e : allInterfaces.entrySet()) {
      String ifaceName = e.getKey();
      Interface iface = e.getValue();
      if (ifaceName.toLowerCase().startsWith("loopback")
          && iface.getActive()
          && iface.getAddress() != null) {
        loopbackInterfaces.put(ifaceName, iface);
      }
    }
    if (loopbackInterfaces.isEmpty()) {
      interfacesToCheck = allInterfaces;
    } else {
      interfacesToCheck = loopbackInterfaces;
    }
    Ip highestIp = Ip.ZERO;
    for (Interface iface : interfacesToCheck.values()) {
      if (!iface.getActive()) {
        continue;
      }
      for (ConcreteInterfaceAddress address : iface.getAllAddresses()) {
        Ip ip = address.getIp();
        if (highestIp.asLong() < ip.asLong()) {
          highestIp = ip;
        }
      }
    }
    return highestIp;
  }

  static void initBgpDefaultRouteExportPolicy(String vrfName, String peerName, Configuration c) {
    SetOrigin setOrigin =
        new SetOrigin(
            new LiteralOrigin(OriginType.IGP , null));
    List<Statement> defaultRouteExportStatements;
    defaultRouteExportStatements =
        ImmutableList.of(setOrigin, Statements.ReturnTrue.toStaticStatement());

    RoutingPolicy.builder()
        .setOwner(c)
        .setName(computeBgpDefaultRouteExportPolicyName(vrfName, peerName))
        .addStatement(
            new If(
                new Conjunction(
                    ImmutableList.of(
                        Common.matchDefaultRoute(), new MatchProtocol(RoutingProtocol.AGGREGATE))),
                defaultRouteExportStatements))
        .addStatement(Statements.ReturnFalse.toStaticStatement())
        .build();
  }


  static @Nonnull CommunitySetAclLine toCommunitySetAclLine(ExpandedCommunityListLine line) {

    String regex = line.getRegex();

    // If the line's regex only requires some community in the set to have a particular format,
    // create a regex on an individual community rather than on the whole set.
    // Regexes on individual communities have a simpler semantics, and some questions
    // (e.g. SearchRoutePolicies) do not handle arbitrary community-set regexes.
    String containsAColon = "(_?\\d+)?:?(\\d+_?)?";
    String noColon = "_?\\d+|\\d+_?";
    String singleCommRegex = containsAColon + "|" + noColon;
    Pattern p = Pattern.compile(singleCommRegex);
    if (p.matcher(regex).matches()) {
      return toCommunitySetAclLineOptimized(line);
    } else {
      return toCommunitySetAclLineUnoptimized(line);
    }
  }
  static @Nonnull CommunitySetAclLine toCommunitySetAclLineOptimized(
      ExpandedCommunityListLine line) {
    return new CommunitySetAclLine(
        line.getAction(), new HasCommunity(toCommunityMatchRegex(line.getRegex())));
  }

  static @Nonnull CommunitySetAclLine toCommunitySetAclLineUnoptimized(
      ExpandedCommunityListLine line) {
    return new CommunitySetAclLine(
        line.getAction(),
        new CommunitySetMatchRegex(
            new TypesFirstAscendingSpaceSeparated(ColonSeparatedRendering.instance()),
            toJavaRegex(line.getRegex())));
  }


  static String toJavaRegex(String ciscoRegex) {
    String withoutQuotes;
    if (ciscoRegex.charAt(0) == '"' && ciscoRegex.charAt(ciscoRegex.length() - 1) == '"') {
      withoutQuotes = ciscoRegex.substring(1, ciscoRegex.length() - 1);
    } else {
      withoutQuotes = ciscoRegex;
    }
    String underscoreReplacement = "(,|\\\\{|\\\\}|^|\\$| )";
    String output = withoutQuotes.replaceAll("_", underscoreReplacement);
    return output;
  }

  static org.batfish.datamodel.StaticRoute toStaticRoute(
      StaticRoute staticRoute, Predicate<Integer> trackExists) {
    String nextHopInterface = staticRoute.getNextHopInterface();
    if (nextHopInterface != null && nextHopInterface.toLowerCase().startsWith("null")) {
      nextHopInterface = org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
    }
    String track =
        Optional.ofNullable(staticRoute.getTrack())
            .filter(trackExists)
            .map(Object::toString)
            .orElse(null);
    return org.batfish.datamodel.StaticRoute.builder()
        .setNetwork(staticRoute.getPrefix())
        .setNextHop(NextHop.legacyConverter(nextHopInterface, staticRoute.getNextHopIp()))
        .setAdministrativeCost(staticRoute.getDistance())
        .setTag(firstNonNull(staticRoute.getTag(), -1L))
        .setTrack(track)
        .build();
  }

  static void computeDistributeListPolicies(
      @Nonnull OspfProcess ospfProcess,
      @Nonnull org.batfish.datamodel.ospf.OspfProcess newOspfProcess,
      @Nonnull Configuration c,
      @Nonnull String vrf,
      @Nonnull String ospfProcessId,
      @Nonnull RgosConfiguration oldConfig,
      @Nonnull Warnings w) {
    DistributeList globalDistributeList = ospfProcess.getInboundGlobalDistributeList();

    BooleanExpr globalCondition = null;
    if (globalDistributeList != null
        && sanityCheckDistributeList(globalDistributeList, c, oldConfig, vrf, ospfProcessId)) {
      globalCondition =
          new MatchPrefixSet(
              DestinationNetwork.instance(),
              new NamedPrefixSet(globalDistributeList.getFilterName()));
    }

    Map<String, DistributeList> interfaceDistributeLists =
        ospfProcess.getInboundInterfaceDistributeLists();

    for (String ifaceName :
        newOspfProcess.getAreas().values().stream()
            .flatMap(a -> a.getInterfaces().stream())
            .collect(Collectors.toList())) {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces(vrf).get(ifaceName);
      DistributeList ifaceDistributeList = interfaceDistributeLists.get(ifaceName);
      BooleanExpr ifaceCondition = null;
      if (ifaceDistributeList != null
          && sanityCheckDistributeList(ifaceDistributeList, c, oldConfig, vrf, ospfProcessId)) {
        ifaceCondition =
            new MatchPrefixSet(
                DestinationNetwork.instance(),
                new NamedPrefixSet(ifaceDistributeList.getFilterName()));
      }

      if (globalCondition == null && ifaceCondition == null) {
        // doing nothing if both global and interface conditions are empty
        continue;
      }

      String policyName = String.format("~OSPF_DIST_LIST_%s_%s_%s~", vrf, ospfProcessId, ifaceName);
      RoutingPolicy routingPolicy = new RoutingPolicy(policyName, c);
      routingPolicy
          .getStatements()
          .add(
              new If(
                  new Conjunction(
                      Stream.of(globalCondition, ifaceCondition)
                          .filter(Objects::nonNull)
                          .collect(ImmutableList.toImmutableList())),
                  ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                  ImmutableList.of(Statements.ExitReject.toStaticStatement())));
      c.getRoutingPolicies().put(routingPolicy.getName(), routingPolicy);
      OspfInterfaceSettings ospfSettings = iface.getOspfSettings();
      if (ospfSettings == null) {
        w.redFlag(
            String.format(
                "Cannot attach inbound distribute list policy '%s' to interface '%s' not"
                    + " configured for OSPF.",
                ifaceName, iface.getName()));
      } else {
        ospfSettings.setInboundDistributeListPolicy(policyName);
      }
    }
  }

  @VisibleForTesting
  static boolean sanityCheckDistributeList(
      @Nonnull DistributeList distributeList,
      @Nonnull Configuration c,
      @Nonnull RgosConfiguration oldConfig,
      String vrfName,
      String ospfProcessId) {
    if (distributeList.getFilterType() != DistributeListFilterType.PREFIX_LIST) {
      // only prefix-lists are supported in distribute-list
      oldConfig
          .getWarnings()
          .redFlag(
              String.format(
                  "OSPF process %s:%s in %s uses distribute-list of type %s, only prefix-lists are"
                      + " supported in dist-lists by Batfish",
                  vrfName, ospfProcessId, oldConfig.getHostname(), distributeList.getFilterType()));
      return false;
    } else if (!c.getRouteFilterLists().containsKey(distributeList.getFilterName())) {
      // if referred prefix-list is not defined, all prefixes will be allowed
      oldConfig
          .getWarnings()
          .redFlag(
              String.format(
                  "dist-list in OSPF process %s:%s uses a prefix-list which is not defined, this"
                      + " dist-list will allow everything",
                  vrfName, ospfProcessId));
      return false;
    }
    return true;
  }

  @VisibleForTesting
  @Nullable
  static org.batfish.datamodel.ospf.OspfNetworkType toOspfNetworkType(
      @Nullable OspfNetworkType type, Warnings warnings) {
    if (type == null) {
      // default is broadcast for all Ethernet interfaces
      // (https://learningnetwork.cisco.com/thread/66827)
      return org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
    }
    switch (type) {
      case BROADCAST:
        return org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
      case POINT_TO_POINT:
        return org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
      case NON_BROADCAST:
        return org.batfish.datamodel.ospf.OspfNetworkType.NON_BROADCAST_MULTI_ACCESS;
      case POINT_TO_MULTIPOINT:
        return org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_MULTIPOINT;
      default:
        warnings.redFlag(
            String.format(
                "Conversion of Cisco OSPF network type '%s' is not handled.", type.toString()));
        return null;
    }
  }


  static @Nonnull BgpAggregate toBgpAggregate(
      BgpAggregateIpv4Network vsAggregate, Configuration c) {
    // TODO: handle as-set
    // TODO: handle suppress-map
    // TODO: verify undefined route-map can be treated as omitted
    String attributeMap =
        Optional.ofNullable(vsAggregate.getAttributeMap())
            .filter(c.getRoutingPolicies()::containsKey)
            .orElse(null);
    return BgpAggregate.of(
        vsAggregate.getPrefix(),
        generateSuppressionPolicy(vsAggregate.getSummaryOnly(), c),
        // TODO: put advertise-map here
        null,
        attributeMap);
  }

  /**
   * Creates a {@link RoutingPolicy} to be used as the BGP export policy for the given {@link
   * LeafBgpPeerGroup}. The generated policy is added to the given configuration's routing policies.
   */
  static void generateBgpExportPolicy(
      LeafBgpPeerGroup lpg, String vrfName, Configuration c, Warnings w) {
    List<Statement> exportPolicyStatements = new ArrayList<>();
    if (lpg.getNextHopSelf() != null && lpg.getNextHopSelf()) {
      exportPolicyStatements.add(new SetNextHop(SelfNextHop.getInstance()));
    }
    if (lpg.getRemovePrivateAs() != null && lpg.getRemovePrivateAs()) {
      exportPolicyStatements.add(Statements.RemovePrivateAs.toStaticStatement());
    }

    // If defaultOriginate is set, generate a default route export policy. Default route will match
    // this policy and get exported without going through the rest of the export policy.
    // TODO Verify that nextHopSelf and removePrivateAs settings apply to default-originate route.
    if (lpg.getDefaultOriginate()) {
      initBgpDefaultRouteExportPolicy(vrfName, lpg.getName(), c);
      exportPolicyStatements.add(
          new If(
              "Export default route from peer with default-originate configured",
              new CallExpr(computeBgpDefaultRouteExportPolicyName(vrfName, lpg.getName())),
              singletonList(Statements.ReturnTrue.toStaticStatement()),
              ImmutableList.of()));
    }

    // Conditions for exporting regular routes (not spawned by default-originate)
    List<BooleanExpr> peerExportConjuncts = new ArrayList<>();
    peerExportConjuncts.add(new CallExpr(generatedBgpCommonExportPolicyName(vrfName)));

    // Add constraints on export routes from configured outbound filter.
    // TODO support configuring multiple outbound filters
    String outboundPrefixListName = lpg.getOutboundPrefixList();
    String outboundRouteMapName = lpg.getOutboundRouteMap();
    String outboundIpAccessListName = lpg.getOutboundIpAccessList();
    if (Stream.of(outboundRouteMapName, outboundPrefixListName, outboundIpAccessListName)
            .filter(Objects::nonNull)
            .count()
        > 1) {
      w.redFlag(
          "Batfish does not support configuring more than one filter"
              + " (route-map/prefix-list/distribute-list) for outgoing BGP routes. When this"
              + " occurs, only the route-map will be used, or the prefix-list if no route-map is"
              + " configured.");
    }
    if (outboundRouteMapName != null) {
      peerExportConjuncts.add(new CallExpr(routeMapOrRejectAll(outboundRouteMapName, c)));
    } else if (outboundPrefixListName != null
        && c.getRouteFilterLists().containsKey(outboundPrefixListName)) {
      peerExportConjuncts.add(
          new MatchPrefixSet(
              DestinationNetwork.instance(), new NamedPrefixSet(outboundPrefixListName)));
    } else if (outboundIpAccessListName != null
        && c.getRouteFilterLists().containsKey(outboundIpAccessListName)) {
      peerExportConjuncts.add(
          new MatchPrefixSet(
              DestinationNetwork.instance(), new NamedPrefixSet(outboundIpAccessListName)));
    }
    exportPolicyStatements.add(
        new If(
            "peer-export policy main conditional: exitAccept if true / exitReject if false",
            new Conjunction(peerExportConjuncts),
            ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
            ImmutableList.of(Statements.ExitReject.toStaticStatement())));
    RoutingPolicy.builder()
        .setOwner(c)
        .setName(generatedBgpPeerExportPolicyName(vrfName, lpg.getName()))
        .setStatements(exportPolicyStatements)
        .build();
  }

  static int toOspfHelloInterval(
      Interface iface, @Nullable org.batfish.datamodel.ospf.OspfNetworkType networkType) {
    Integer helloInterval = iface.getOspfHelloInterval();
    if (helloInterval != null) {
      return helloInterval;
    }
    if (networkType == POINT_TO_POINT || networkType == BROADCAST) {
      return DEFAULT_OSPF_HELLO_INTERVAL_P2P_AND_BROADCAST;
    }
    return DEFAULT_OSPF_HELLO_INTERVAL;
  }

  /**
   * Returns the name of a {@link RoutingPolicy} to be used as the BGP import policy for the given
   * {@link LeafBgpPeerGroup}, or {@code null} if no constraints are imposed on the peer's inbound
   * routes. When a nonnull policy name is returned, the corresponding policy is guaranteed to exist
   * in the given configuration's routing policies.
   */
  @Nullable
  static String generateBgpImportPolicy(
      LeafBgpPeerGroup lpg, String vrfName, Configuration c, Warnings w) {
    // TODO Support filter-list
    // https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/5816-bgpfaq-5816.html

    String inboundRouteMapName = lpg.getInboundRouteMap();
    String inboundPrefixListName = lpg.getInboundPrefixList();
    String inboundIpAccessListName = lpg.getInboundIpAccessList();

    // TODO Support using multiple filters in BGP import policies
    if (Stream.of(inboundRouteMapName, inboundPrefixListName, inboundIpAccessListName)
            .filter(Objects::nonNull)
            .count()
        > 1) {
      w.redFlag(
          "Batfish does not support configuring more than one filter"
              + " (route-map/prefix-list/distribute-list) for incoming BGP routes. When this"
              + " occurs, only the route-map will be used, or the prefix-list if no route-map is"
              + " configured.");
    }

    // Warnings for references to undefined route-maps and prefix-lists will be surfaced elsewhere.
    if (inboundRouteMapName != null) {
      // Inbound route-map is defined. Use that as the BGP import policy.
      return routeMapOrRejectAll(inboundRouteMapName, c);
    }

    String exportRouteFilter = null;
    if (inboundPrefixListName != null
        && c.getRouteFilterLists().containsKey(inboundPrefixListName)) {
      exportRouteFilter = inboundPrefixListName;
    } else if (inboundIpAccessListName != null
        && c.getRouteFilterLists().containsKey(inboundIpAccessListName)) {
      exportRouteFilter = inboundIpAccessListName;
    }

    if (exportRouteFilter != null) {
      // Inbound prefix-list or distribute-list is defined. Build an import policy around it.
      String generatedImportPolicyName = computeBgpPeerImportPolicyName(vrfName, lpg.getName());
      RoutingPolicy.builder()
          .setOwner(c)
          .setName(generatedImportPolicyName)
          .addStatement(
              new If(
                  new MatchPrefixSet(
                      DestinationNetwork.instance(), new NamedPrefixSet(exportRouteFilter)),
                  ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                  ImmutableList.of(Statements.ExitReject.toStaticStatement())))
          .build();
      return generatedImportPolicyName;
    }
    // Return null to indicate no constraints were imposed on inbound BGP routes.
    return null;
  }

  private static @Nullable String routeMapOrRejectAll(@Nullable String mapName, Configuration c) {
    if (mapName == null || c.getRoutingPolicies().containsKey(mapName)) {
      return mapName;
    }
    String undefinedName = mapName + "~undefined";
    if (!c.getRoutingPolicies().containsKey(undefinedName)) {
      // For undefined route-map, generate a route-map that denies everything.
      RoutingPolicy.builder()
          .setName(undefinedName)
          .addStatement(ROUTE_MAP_DENY_STATEMENT)
          .setOwner(c)
          .build();
    }
    return undefinedName;
  }
  private static final Statement ROUTE_MAP_DENY_STATEMENT =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
          ImmutableList.of(Statements.ExitReject.toStaticStatement()));




  static int toOspfDeadInterval(
      Interface iface, @Nullable org.batfish.datamodel.ospf.OspfNetworkType networkType) {
    Integer deadInterval = iface.getOspfDeadInterval();
    if (deadInterval != null) {
      return deadInterval;
    }
    Integer helloInterval = iface.getOspfHelloInterval();
    if (helloInterval != null) {
      return OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * helloInterval;
    }
    if (networkType == POINT_TO_POINT || networkType == BROADCAST) {
      return DEFAULT_OSPF_DEAD_INTERVAL_P2P_AND_BROADCAST;
    }
    return DEFAULT_OSPF_DEAD_INTERVAL;
  }
  // prevent instantiation
  private RgosConversions() {}
}
