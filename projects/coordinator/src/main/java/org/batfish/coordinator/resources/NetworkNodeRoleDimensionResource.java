package org.batfish.coordinator.resources;

import static org.batfish.common.util.HttpUtil.checkClientArgument;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.batfish.coordinator.Main;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;

/**
 * The {@link NetworkNodeRoleDimensionResource} is a resource for servicing client API calls for
 * node role dimensions. It is a subresource of {@link NetworkNodeRolesResource}.
 *
 * <p>This resource provides information about the role dimension using GET. It also allows
 * modifications and creating of new dimensions using PUT.
 */
@Produces(MediaType.APPLICATION_JSON)
@ParametersAreNonnullByDefault
public final class NetworkNodeRoleDimensionResource {

  private final String _network;
  private final String _dimension;

  public NetworkNodeRoleDimensionResource(String network, String dimension) {
    _network = network;
    _dimension = dimension;
  }

  @DELETE
  public Response delNodeRoleDimension() throws IOException {
    NodeRolesData nodeRolesData = Main.getWorkMgr().getNetworkNodeRoles(_network);
    if (nodeRolesData == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Optional<NodeRoleDimension> dimension = nodeRolesData.getNodeRoleDimension(_dimension);
    if (!dimension.isPresent()) {
      return Response.status(Status.NOT_FOUND).build();
    }
    if (!Main.getWorkMgr()
        .putNetworkNodeRoles(
            NodeRolesData.builder()
                .setDefaultDimension(nodeRolesData.getDefaultDimension())
                .setRoleDimensions(
                    nodeRolesData.getNodeRoleDimensions().stream()
                        .filter(dim -> !dim.getName().equalsIgnoreCase(dimension.get().getName()))
                        .collect(ImmutableList.toImmutableList()))
                .build(),
            _network)) {
      // if network was deleted while we were working
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().build();
  }

  @GET
  public Response getNodeRoleDimension() throws IOException {
    NodeRolesData nodeRolesData = Main.getWorkMgr().getNetworkNodeRoles(_network);
    if (nodeRolesData == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Optional<NodeRoleDimension> dimension = nodeRolesData.getNodeRoleDimension(_dimension);
    if (!dimension.isPresent()) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().entity(new NodeRoleDimensionBean(dimension.get(), null)).build();
  }

  /**
   * Adds the supplied node role dimension. If one of the same name already exists, it is
   * overwritten.
   */
  @PUT
  public Response putNodeRoleDimension(NodeRoleDimensionBean dimBean) throws IOException {
    checkClientArgument(dimBean.name != null, "Node role dimension must have a name");
    NodeRolesData nodeRolesData = Main.getWorkMgr().getNetworkNodeRoles(_network);
    if (nodeRolesData == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    if (!Main.getWorkMgr()
        .putNetworkNodeRoles(
            NodeRolesData.builder()
                .setDefaultDimension(nodeRolesData.getDefaultDimension())
                .setRoleDimensions(
                    ImmutableList.<NodeRoleDimension>builder()
                        .addAll(
                            nodeRolesData.getNodeRoleDimensions().stream()
                                .filter(d -> !d.getName().equalsIgnoreCase(dimBean.name))
                                .collect(Collectors.toList()))
                        .add(dimBean.toNodeRoleDimension())
                        .build())
                .build(),
            _network)) {
      // if network was deleted while we were working
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok().build();
  }
}
