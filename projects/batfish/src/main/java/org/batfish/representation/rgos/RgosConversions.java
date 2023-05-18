package org.batfish.representation.rgos;

import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;

/**
 * Utility class for converting {@link RgosConfiguration} to vendor-independent {@link
 * Configuration}.
 */
public final class RgosConversions {

  static void convertStaticRoutes(RgosConfiguration vc, Configuration c) {
    vc.getStaticRoutes()
        .forEach(
            (prefix, route) ->
                c.getDefaultVrf().getStaticRoutes().add(toStaticRoute(prefix, route)));
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
