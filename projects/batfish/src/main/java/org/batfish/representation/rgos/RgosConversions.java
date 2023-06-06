package org.batfish.representation.rgos;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAclLine;
import org.batfish.datamodel.routing_policy.communities.ColonSeparatedRendering;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchRegex;
import org.batfish.datamodel.routing_policy.communities.TypesFirstAscendingSpaceSeparated;

import org.batfish.datamodel.routing_policy.communities.HasCommunity;

import java.util.regex.Pattern;

/**
 * Utility class for converting {@link RgosConfiguration} to vendor-independent {@link
 * Configuration}.
 */
public final class RgosConversions {

  static AsPathAccessList toAsPathAccessList(IpAsPathAccessList pathList) {
    List<AsPathAccessListLine> lines =
        pathList.getLines().stream()
            .map(IpAsPathAccessListLine::toAsPathAccessListLine)
            .collect(ImmutableList.toImmutableList());
    return new AsPathAccessList(pathList.getName(), lines);
  }


  static void convertStaticRoutes(RgosConfiguration vc, Configuration c) {
    vc.getStaticRoutes()
        .forEach(
            (prefix, route) ->
                c.getDefaultVrf().getStaticRoutes().add(toStaticRoute(prefix, route)));
  }
  static @Nonnull CommunityMatchRegex toCommunityMatchRegex(String regex) {
    return new CommunityMatchRegex(ColonSeparatedRendering.instance(), toJavaRegex(regex));
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

  private static @Nonnull org.batfish.datamodel.StaticRoute toStaticRoute(
      Prefix prefix, StaticRoute route) {
    return org.batfish.datamodel.StaticRoute.builder()
        .setAdministrativeCost(1)
        .setMetric(0L)
        .setNextHop(NEXT_HOP_CONVERTER.visit(route.getNextHop()))
        .setNetwork(prefix)
        .build();
  }

  private static final NextHopVisitor<org.batfish.datamodel.route.nh.NextHop> NEXT_HOP_CONVERTER =
      new NextHopVisitor<org.batfish.datamodel.route.nh.NextHop>() {
        @Override
        public org.batfish.datamodel.route.nh.NextHop visitNextHopDiscard(
            NextHopDiscard nextHopDiscard) {
          return org.batfish.datamodel.route.nh.NextHopDiscard.instance();
        }

        @Override
        public org.batfish.datamodel.route.nh.NextHop visitNextHopGateway(
            NextHopGateway nextHopGateway) {
          return org.batfish.datamodel.route.nh.NextHopIp.of(nextHopGateway.getIp());
        }

        @Override
        public org.batfish.datamodel.route.nh.NextHop visitNextHopInterface(
            NextHopInterface nextHopInterface) {
          return org.batfish.datamodel.route.nh.NextHopInterface.of(
              nextHopInterface.getInterface());
        }
      };

  // prevent instantiation
  private RgosConversions() {}
}
