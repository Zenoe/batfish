package org.batfish.representation.rgos;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.communities.AllStandardCommunities;
import org.batfish.datamodel.routing_policy.communities.CommunitySetDifference;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.statement.Statement;

public class RouteMapSetCommunityNoneLine extends RouteMapSetLine {

  @Override
  public void applyTo(
      List<Statement> statements, RgosConfiguration cc, Configuration c, Warnings w) {
    statements.add(
        new SetCommunities(
            new CommunitySetDifference(
                InputCommunities.instance(), AllStandardCommunities.instance())));
  }
}
