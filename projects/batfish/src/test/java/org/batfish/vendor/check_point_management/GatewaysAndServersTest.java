package org.batfish.vendor.check_point_management;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.Ip;
import org.junit.Test;

/** Test of {@link GatewaysAndServers}. */
public final class GatewaysAndServersTest {
  @Test
  public void testJacksonDeserialization() throws JsonProcessingException {
    String input =
        "{"
            + "\"GARBAGE\":0,"
            + "\"objects\":["
            + "{" // object: CpmiClusterMember
            + "\"type\":\"CpmiClusterMember\","
            + "\"uid\":\"0\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiClusterMember
            + "{" // object: CpmiGatewayCluster
            + "\"type\":\"CpmiGatewayCluster\","
            + "\"uid\":\"1\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiGatewayCluster
            + "{" // object: CpmiHostCkp
            + "\"type\":\"CpmiHostCkp\","
            + "\"uid\":\"2\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiHostCkp
            + "{" // object: CpmiVsClusterNetobj
            + "\"type\":\"CpmiVsClusterNetobj\","
            + "\"uid\":\"3\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiVsClusterNetobj
            + "{" // object: CpmiVsNetobj
            + "\"type\":\"CpmiVsNetobj\","
            + "\"uid\":\"4\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiVsNetobj
            + "{" // object: CpmiVsxClusterMember
            + "\"type\":\"CpmiVsxClusterMember\","
            + "\"uid\":\"5\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiVsxClusterMember
            + "{" // object: CpmiVsxClusterNetobj
            + "\"type\":\"CpmiVsxClusterNetobj\","
            + "\"uid\":\"6\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiVsxClusterNetobj
            + "{" // object: CpmiVsxNetobj
            + "\"type\":\"CpmiVsxNetobj\","
            + "\"uid\":\"7\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}," // object: CpmiVsxNetobj
            + "{" // object: simple-gateway
            + "\"type\":\"simple-gateway\","
            + "\"uid\":\"8\","
            + "\"name\":\"foo\","
            + "\"ipv4-address\":\"0.0.0.0\""
            + "}" // object: simple-gateway
            + "]" // objects
            + "}"; // GatewaysAndServers
    assertThat(
        BatfishObjectMapper.ignoreUnknownMapper().readValue(input, GatewaysAndServers.class),
        equalTo(
            new GatewaysAndServers(
                ImmutableMap.<Uid, GatewayOrServer>builder()
                    .put(Uid.of("0"), new CpmiClusterMember(Ip.ZERO, "foo", Uid.of("0")))
                    .put(Uid.of("1"), new CpmiGatewayCluster(Ip.ZERO, "foo", Uid.of("1")))
                    .put(Uid.of("2"), new CpmiHostCkp(Ip.ZERO, "foo", Uid.of("2")))
                    .put(Uid.of("3"), new CpmiVsClusterNetobj(Ip.ZERO, "foo", Uid.of("3")))
                    .put(Uid.of("4"), new CpmiVsNetobj(Ip.ZERO, "foo", Uid.of("4")))
                    .put(Uid.of("5"), new CpmiVsxClusterMember(Ip.ZERO, "foo", Uid.of("5")))
                    .put(Uid.of("6"), new CpmiVsxClusterNetobj(Ip.ZERO, "foo", Uid.of("6")))
                    .put(Uid.of("7"), new CpmiVsxNetobj(Ip.ZERO, "foo", Uid.of("7")))
                    .put(Uid.of("8"), new SimpleGateway(Ip.ZERO, "foo", Uid.of("8")))
                    .build())));
  }

  @Test
  public void testJavaSerialization() {
    GatewaysAndServers obj =
        new GatewaysAndServers(
            ImmutableMap.of(Uid.of("0"), new SimpleGateway(Ip.ZERO, "foo", Uid.of("0"))));
    assertEquals(obj, SerializationUtils.clone(obj));
  }

  @Test
  public void testEquals() {
    GatewaysAndServers obj = new GatewaysAndServers(ImmutableMap.of());
    new EqualsTester()
        .addEqualityGroup(obj, new GatewaysAndServers(ImmutableMap.of()))
        .addEqualityGroup(
            new GatewaysAndServers(
                ImmutableMap.of(Uid.of("0"), new SimpleGateway(Ip.ZERO, "foo", Uid.of("0")))))
        .testEquals();
  }
}
