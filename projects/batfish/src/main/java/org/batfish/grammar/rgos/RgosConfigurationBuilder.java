package org.batfish.grammar.rgos;

import org.batfish.representation.rgos.OspfWildcardNetwork;

import com.google.common.collect.Range;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import org.batfish.common.BatfishException;
import org.batfish.common.Warnings.ParseWarning;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Configuration;
import static org.batfish.datamodel.Names.bgpNeighborStructureName;
import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.rgos.RgosStructureType.INTERFACE;
import static org.batfish.representation.rgos.RgosStructureUsage.INTERFACE_SELF_REF;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_NEIGHBOR_SELF_REF;
import static org.batfish.representation.rgos.RgosStructureType.BGP_NEIGHBOR;
import static org.batfish.representation.rgos.RgosStructureType.BGP_UNDECLARED_PEER;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_NEIGHBOR_WITHOUT_REMOTE_AS;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_NEIGHBOR_WITHOUT_REMOTE_AS;
import static org.batfish.representation.rgos.RgosStructureType.BGP_PEER_GROUP;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_PEER_GROUP_REFERENCED_BEFORE_DEFINED;
import static org.batfish.representation.rgos.RgosStructureType.BGP_UNDECLARED_PEER_GROUP;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_NEIGHBOR_STATEMENT;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_UPDATE_SOURCE_INTERFACE;
import static org.batfish.representation.rgos.RgosStructureType.ROUTE_MAP;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_NETWORK6_ORIGINATION_ROUTE_MAP;
import static org.batfish.representation.rgos.RgosStructureUsage.BGP_NETWORK_ORIGINATION_ROUTE_MAP;

import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.SilentSyntaxListener;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.rgos.RgosParser.Neighbor_flat_rb_stanzaContext;
import org.batfish.grammar.rgos.RgosParser.Address_family_rb_stanzaContext;
import org.batfish.grammar.rgos.RgosParser.Router_id_bgp_tailContext;
import org.batfish.grammar.rgos.RgosParser.Uint16Context;
import org.batfish.grammar.rgos.RgosParser.Uint32Context;
import org.batfish.grammar.rgos.RgosParser.Remote_as_bgp_tailContext;
import org.batfish.grammar.rgos.RgosParser.Update_source_bgp_tailContext;
import org.batfish.grammar.rgos.RgosParser.S_router_ospfContext;
import org.batfish.grammar.rgos.RgosParser.Activate_bgp_tailContext;
import org.batfish.grammar.rgos.RgosParser.Ro_networkContext;

import org.batfish.grammar.rgos.RgosParser.Network_bgp_tailContext;
import org.batfish.grammar.rgos.RgosParser.Network6_bgp_tailContext;
import org.batfish.grammar.rgos.RgosParser.Address_family_rb_stanzaContext;
import org.batfish.grammar.rgos.RgosParser.S_interface_definitionContext;
import org.batfish.grammar.rgos.RgosParser.S_hostnameContext;
import org.batfish.grammar.rgos.RgosParser.Interface_nameContext;
import org.batfish.grammar.rgos.RgosParser.If_ip_addressContext;
import org.batfish.grammar.rgos.RgosParser.RangeContext;
import org.batfish.grammar.rgos.RgosParser.RangeContext;
import org.batfish.grammar.rgos.RgosParser.SubrangeContext;
import org.batfish.grammar.rgos.RgosParser.Router_bgp_stanzaContext;
import org.batfish.grammar.rgos.RgosParser.Bgp_asnContext;
import org.batfish.grammar.rgos.RgosParser.Uint8Context;
import org.batfish.grammar.rgos.RgosParser.DecContext;
import org.batfish.grammar.rgos.RgosParser.Vlan_idContext;

import org.batfish.representation.rgos.RgosConfiguration;
import org.batfish.representation.rgos.BgpPeerGroup;
import org.batfish.representation.rgos.MasterBgpPeerGroup;
import org.batfish.representation.rgos.IpBgpPeerGroup;
import org.batfish.representation.rgos.Ipv6BgpPeerGroup;
import org.batfish.representation.rgos.NamedBgpPeerGroup;
import org.batfish.representation.rgos.BgpNetwork;
import org.batfish.representation.rgos.BgpNetwork6;
import org.batfish.representation.rgos.Interface;
import org.batfish.representation.rgos.Vrf;
import org.batfish.representation.rgos.BgpProcess;
import org.batfish.representation.rgos.OspfProcess;

import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;

@ParametersAreNonnullByDefault
public final class RgosConfigurationBuilder extends RgosParserBaseListener
    implements SilentSyntaxListener {

  public RgosConfigurationBuilder(
      RgosCombinedParser parser,
      String text,
      Warnings warnings,
      SilentSyntaxCollection silentSyntax) {
    _parser = parser;
    _text = text;
    _configuration = new RgosConfiguration();
    _configuration.setExtraLines(_parser.getExtraLines());
    _w = warnings;
    _silentSyntax = silentSyntax;
    _peerGroupStack = new ArrayList<>();

  }

  /**
   * Convert a {@link ParserRuleContext} whose text is guaranteed to represent a valid signed 32-bit
   * decimal integer to an {@link Integer} if it is contained in the provided {@code space}, or else
   * {@link Optional#empty}.
   *
   * <p>This function should only be called by more strictly typed overloads of {@code
   * toIntegerSpace}.
   */
  private @Nonnull Optional<Integer> toIntegerInSpace_helper(
      ParserRuleContext messageCtx, ParserRuleContext ctx, IntegerSpace space, String name) {
    int num = Integer.parseInt(ctx.getText());
    if (!space.contains(num)) {
      warn(messageCtx, String.format("Expected %s in range %s, but got '%d'", name, space, num));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  private static @Nonnull String unquote(String text) {
    if (text.length() == 0) {
      return text;
    }
    if (text.charAt(0) != '"') {
      return text;
    }
    // Here for sanity, but should not trigger unless the definition of string rule is broken.
    checkArgument(text.charAt(text.length() - 1) == '"', "Improperly-quoted string: %s", text);
    return text.substring(1, text.length() - 1);
  }

  private void popPeer() {
    int index = _peerGroupStack.size() - 1;
    _currentPeerGroup = _peerGroupStack.get(index);
    _peerGroupStack.remove(index);
    _inIpv6BgpPeer = false;
  }

  private void pushPeer(@Nonnull BgpPeerGroup pg) {
    _peerGroupStack.add(_currentPeerGroup);
    _currentPeerGroup = pg;
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    _configuration.setUnrecognized(true);

    if (token instanceof UnrecognizedLineToken) {
      UnrecognizedLineToken unrecToken = (UnrecognizedLineToken) token;
      _w.getParseWarnings()
          .add(
              new ParseWarning(
                  line, lineText, unrecToken.getParserContext(), "This syntax is unrecognized"));
    } else {
      String msg = String.format("Unrecognized Line: %d: %s", line, lineText);
      _w.redFlag(msg + " SUBSEQUENT LINES MAY NOT BE PROCESSED CORRECTLY");
    }
  }

  @Override
  public void exitS_hostname(S_hostnameContext ctx) {
    String hostname;
    if (ctx.quoted_name != null) {
      hostname = unquote(ctx.quoted_name.getText());
    } else {
      StringBuilder sb = new StringBuilder();
      for (Token namePart : ctx.name_parts) {
        sb.append(namePart.getText());
      }
      hostname = sb.toString();
    }
    _configuration.setHostname(hostname);
    _configuration.getRf().setHostname(hostname);
  }

  @Override
  public void enterS_interface_definition(S_interface_definitionContext ctx) {
    String nameAlpha = ctx.iname.name_prefix_alpha.getText();
    String canonicalNamePrefix;
    try {
      canonicalNamePrefix = RgosConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    } catch (BatfishException e) {
      warn(ctx, "Error fetching interface name: " + e.getMessage());
      _currentInterfaces = ImmutableList.of();
      return;
    }
    StringBuilder namePrefix = new StringBuilder(canonicalNamePrefix);
    for (Token part : ctx.iname.name_middle_parts) {
      namePrefix.append(part.getText());
    }
    _currentInterfaces = new ArrayList<>();
    if (ctx.iname.range() != null) {
      List<SubRange> ranges = toRange(ctx.iname.range());
      for (SubRange range : ranges) {
        for (int i = range.getStart(); i <= range.getEnd(); i++) {
          String name = namePrefix.toString() + i;
          addInterface(name, ctx.iname, true);
          _configuration.defineStructure(INTERFACE, name, ctx);
          System.out.println("ctx.getStart().getLine()--------" + ctx.getStart().getLine());
          _configuration.referenceStructure(
              INTERFACE, name, INTERFACE_SELF_REF, ctx.getStart().getLine());
        }
      }
    } else {
      addInterface(namePrefix.toString(), ctx.iname, true);
    }
    if (ctx.MULTIPOINT() != null) {
      todo(ctx);
    }
  }

  @Override
  public void exitS_interface_definition(S_interface_definitionContext ctx) {
    _currentInterfaces = null;
  }

  @Override
  public void enterRouter_bgp_stanza(Router_bgp_stanzaContext ctx) {
    long procNum = ctx.bgp_asn() == null ? 0 : toAsNum(ctx.bgp_asn());
    Vrf vrf = _configuration.getVrfs().get(Configuration.DEFAULT_VRF_NAME);
    if (vrf.getBgpProcess() == null) {
      BgpProcess proc = new BgpProcess(procNum);
      vrf.setBgpProcess(proc);
    }
    BgpProcess proc = vrf.getBgpProcess();
    if (proc.getProcnum() != procNum && procNum != 0) {
      warn(ctx, "Cannot have multiple BGP processes with different ASNs");
      pushPeer(_dummyPeerGroup);
      return;
    }
    pushPeer(proc.getMasterBgpPeerGroup());
  }

  public void enterNeighbor_flat_rb_stanza(Neighbor_flat_rb_stanzaContext ctx) {
    if (ctx.ip6 != null) {
      // Remember we are in IPv6 context so that structure references are identified accordingly
      _inIpv6BgpPeer = true;
    }
    // do no further processing for unsupported address families / containers
    if (_currentPeerGroup == _dummyPeerGroup) {
      pushPeer(_dummyPeerGroup);
      return;
    }
    BgpProcess proc = currentVrf().getBgpProcess();
    // we must create peer group if it does not exist and this is a remote_as
    // declaration
    boolean create =
        ctx.remote_as_bgp_tail() != null || ctx.inherit_peer_session_bgp_tail() != null;
    if (ctx.ip != null) {
      Ip ip = toIp(ctx.ip);
      _currentIpPeerGroup = proc.getIpPeerGroups().get(ip);
      String bgpNeighborStructName =
          bgpNeighborStructureName(ip.toString(), currentVrf().getName());
      if (_currentIpPeerGroup == null) {
        if (create) {
          _currentIpPeerGroup = proc.addIpPeerGroup(ip);
          pushPeer(_currentIpPeerGroup);
          _configuration.defineStructure(BGP_NEIGHBOR, bgpNeighborStructName, ctx);
          _configuration.referenceStructure(
              BGP_NEIGHBOR, bgpNeighborStructName, BGP_NEIGHBOR_SELF_REF, ctx.ip.getLine());
        } else {
          _configuration.referenceStructure(
              BGP_UNDECLARED_PEER,
              bgpNeighborStructName,
              BGP_NEIGHBOR_WITHOUT_REMOTE_AS,
              ctx.ip.getLine());
          pushPeer(_dummyPeerGroup);
        }
      } else {
        pushPeer(_currentIpPeerGroup);
        _configuration.defineStructure(BGP_NEIGHBOR, bgpNeighborStructName, ctx);
        _configuration.referenceStructure(
            BGP_NEIGHBOR, bgpNeighborStructName, BGP_NEIGHBOR_SELF_REF, ctx.ip.getLine());
      }
    } else if (ctx.ip6 != null) {
      Ip6 ip6 = toIp6(ctx.ip6);
      Ipv6BgpPeerGroup pg6 = proc.getIpv6PeerGroups().get(ip6);
      String bgpNeighborStructName =
          bgpNeighborStructureName(ip6.toString(), currentVrf().getName());
      if (pg6 == null) {
        if (create) {
          pg6 = proc.addIpv6PeerGroup(ip6);
          pushPeer(pg6);
          _configuration.defineStructure(BGP_NEIGHBOR, bgpNeighborStructName, ctx);
          _configuration.referenceStructure(
              BGP_NEIGHBOR, bgpNeighborStructName, BGP_NEIGHBOR_SELF_REF, ctx.ip6.getLine());
        } else {
          _configuration.referenceStructure(
              BGP_UNDECLARED_PEER,
              bgpNeighborStructName,
              BGP_NEIGHBOR_WITHOUT_REMOTE_AS,
              ctx.ip6.getLine());
          pushPeer(_dummyPeerGroup);
        }
      } else {
        pushPeer(pg6);
        _configuration.defineStructure(BGP_NEIGHBOR, bgpNeighborStructName, ctx);
        _configuration.referenceStructure(
            BGP_NEIGHBOR, bgpNeighborStructName, BGP_NEIGHBOR_SELF_REF, ctx.ip6.getLine());
      }
      _currentIpv6PeerGroup = pg6;
    } else if (ctx.peergroup != null) {
      String name = ctx.peergroup.getText();
      _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
      if (_currentNamedPeerGroup == null) {
        if (create) {
          _currentNamedPeerGroup = proc.addNamedPeerGroup(name);
          _configuration.referenceStructure(
              BGP_PEER_GROUP, name, BGP_NEIGHBOR_STATEMENT, ctx.peergroup.getLine());
        } else {
          _configuration.referenceStructure(
              BGP_UNDECLARED_PEER_GROUP,
              name,
              BGP_PEER_GROUP_REFERENCED_BEFORE_DEFINED,
              ctx.peergroup.getLine());
          _currentNamedPeerGroup = new NamedBgpPeerGroup("dummy");
        }
      }
      pushPeer(_currentNamedPeerGroup);
    } else {
      throw new BatfishException("unknown neighbor type");
    }
  }

  @Override
  public void exitRouter_bgp_stanza(Router_bgp_stanzaContext ctx) {
    popPeer();
  }

  @Override
  public void exitIf_ip_address(If_ip_addressContext ctx) {
    ConcreteInterfaceAddress address;
    if (ctx.prefix != null) {
      address = ConcreteInterfaceAddress.parse(ctx.prefix.getText());
    } else {
      Ip ip = toIp(ctx.ip);
      Ip mask = toIp(ctx.subnet);
      address = ConcreteInterfaceAddress.create(ip, mask);
    }
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setAddress(address);
    }
    if (ctx.STANDBY() != null) {
      Ip standbyIp = toIp(ctx.standby_address);
      ConcreteInterfaceAddress standbyAddress =
          ConcreteInterfaceAddress.create(standbyIp, address.getNetworkBits());
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setStandbyAddress(standbyAddress);
      }
    }
    if (ctx.ROUTE_PREFERENCE() != null) {
      warn(ctx, "Unsupported: route-preference declared in interface IP address");
    }
    if (ctx.TAG() != null) {
      warn(ctx, "Unsupported: tag declared in interface IP address");
    }
  }

  @Override
  public void exitAddress_family_rb_stanza(Address_family_rb_stanzaContext ctx) {
    if (ctx.address_family_header() != null
        && ctx.address_family_header().af != null
        && ctx.address_family_header().af.vrf_name != null) {
      _currentVrf = Configuration.DEFAULT_VRF_NAME;
    }
    popPeer();
  }

  @Override
  public void exitRouter_id_bgp_tail(Router_id_bgp_tailContext ctx) {
    Ip routerId = toIp(ctx.routerid);
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.setRouterId(routerId);
  }

  @Override
  public void exitRemote_as_bgp_tail(Remote_as_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      throw new BatfishException(
          "no peer or peer group in context: " + getLocation(ctx) + getFullText(ctx));
    }
    long as = toAsNum(ctx.remote);
    _currentPeerGroup.setRemoteAs(as);
    if (ctx.alt_ases != null) {
      _currentPeerGroup.setAlternateAs(
          ctx.alt_ases.stream()
              .map(RgosConfigurationBuilder::toAsNum)
              .collect(ImmutableSet.toImmutableSet()));
    }
  }

  @Override
  public void exitUpdate_source_bgp_tail(Update_source_bgp_tailContext ctx) {
    String source = toInterfaceName(ctx.source);
    _configuration.referenceStructure(
        INTERFACE, source, BGP_UPDATE_SOURCE_INTERFACE, ctx.getStart().getLine());

    if (_currentPeerGroup != null) {
      _currentPeerGroup.setUpdateSource(source);
    }
  }

  @Override
  public void exitNetwork_bgp_tail(Network_bgp_tailContext ctx) {
    Prefix prefix;
    if (ctx.prefix != null) {
      prefix = Prefix.parse(ctx.prefix.getText());
    } else {
      Ip address = toIp(ctx.ip);
      Ip mask = (ctx.mask != null) ? toIp(ctx.mask) : address.getClassMask();
      int prefixLength = mask.numSubnetBits();
      prefix = Prefix.create(address, prefixLength);
    }
    String map = null;
    if (ctx.mapname != null) {
      map = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, map, BGP_NETWORK_ORIGINATION_ROUTE_MAP, ctx.mapname.getStart().getLine());
    }
    BgpNetwork bgpNetwork = new BgpNetwork(map);
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.getIpNetworks().put(prefix, bgpNetwork);
  }

  @Override
  public void exitNetwork6_bgp_tail(Network6_bgp_tailContext ctx) {
    Prefix6 prefix6 = Prefix6.parse(ctx.prefix.getText());
    String map = null;
    if (ctx.mapname != null) {
      map = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, map, BGP_NETWORK6_ORIGINATION_ROUTE_MAP, ctx.mapname.getStart().getLine());
    }
    BgpProcess proc = currentVrf().getBgpProcess();
    BgpNetwork6 bgpNetwork6 = new BgpNetwork6(map);
    proc.getIpv6Networks().put(prefix6, bgpNetwork6);
  }

  @Override
  public void exitActivate_bgp_tail(Activate_bgp_tailContext ctx) {
    if (_currentPeerGroup == null) {
      return;
    }
    BgpProcess proc = currentVrf().getBgpProcess();
    if (_currentPeerGroup != proc.getMasterBgpPeerGroup()) {
      _currentPeerGroup.setActive(true);
    } else {
      throw new BatfishException("no peer or peer group to activate in this context");
    }
  }

  @Override
  public void exitS_router_ospf(S_router_ospfContext ctx) {
    _currentOspfProcess = null;
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
  }

  public void exitRo_network(Ro_networkContext ctx) {
    Ip address;
    Ip wildcard;
    if (ctx.prefix != null) {
      Prefix prefix = Prefix.parse(ctx.prefix.getText());
      address = prefix.getStartIp();
      wildcard = prefix.getPrefixWildcard();
    } else {
      address = toIp(ctx.ip);
      wildcard = toIp(ctx.wildcard);
    }
    long area;
    if (ctx.area_int != null) {
      area = toLong(ctx.area_int);
    } else if (ctx.area_ip != null) {
      area = toIp(ctx.area_ip).asLong();
    } else {
      throw new BatfishException("bad area");
    }
    OspfWildcardNetwork network = new OspfWildcardNetwork(address, wildcard, area);
    _currentOspfProcess.getWildcardNetworks().add(network);
  }


  @Override
  public void exitEveryRule(ParserRuleContext ctx) {
    tryProcessSilentSyntax(ctx);
  }

  @Override
  public @Nonnull SilentSyntaxCollection getSilentSyntax() {
    return _silentSyntax;
  }

  @Override
  public @Nonnull String getInputText() {
    return _text;
  }

  @Override
  public @Nonnull BatfishCombinedParser<?, ?> getParser() {
    return _parser;
  }

  @Override
  public @Nonnull Warnings getWarnings() {
    return _w;
  }

  public @Nonnull RgosConfiguration getConfiguration() {
    return _configuration;
  }

  private String getLocation(ParserRuleContext ctx) {
    return ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ": ";
  }

  private Vrf currentVrf() {
    return initVrf(_currentVrf);
  }

  private Interface addInterface(String name, Interface_nameContext ctx, boolean explicit) {
    Interface newInterface = _configuration.getInterfaces().get(name);
    if (newInterface == null) {
      newInterface = new Interface(name, _configuration);
      _configuration.getInterfaces().put(name, newInterface);
      initInterface(newInterface, ctx);
    } else {
      _w.pedantic("Interface: '" + name + "' altered more than once");
    }
    // newInterface.setDeclaredNames(
    //     new ImmutableSortedSet.Builder<String>(naturalOrder())
    //         .addAll(newInterface.getDeclaredNames())
    //         .add(ctx.getText())
    //         .build());
    if (explicit) {
      _currentInterfaces.add(newInterface);
    }
    return newInterface;
  }


  private void initInterface(Interface iface, Interface_nameContext ctx) {
    System.out.println("initinterface----");
    String nameAlpha = ctx.name_prefix_alpha.getText();
    String canonicalNamePrefix = RgosConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    String vrf =
        canonicalNamePrefix.equals(RgosConfiguration.MANAGEMENT_INTERFACE_PREFIX)
            ? RgosConfiguration.MANAGEMENT_VRF_NAME
            : RgosConfiguration.DEFAULT_VRF_NAME;
    int mtu = Interface.getDefaultMtu();
    iface.setVrf(vrf);
    initVrf(vrf);
    iface.setMtu(mtu);
  }

  private Vrf initVrf(String vrfName) {
    return _configuration.getVrfs().computeIfAbsent(vrfName, Vrf::new);
  }
  private static Ip toIp(TerminalNode t) {
    return Ip.parse(t.getText());
  }

  private static Ip toIp(Token t) {
    return Ip.parse(t.getText());
  }

  private static Ip6 toIp6(Token t) {
    return Ip6.parse(t.getText());
  }

  private static long toAsNum(Bgp_asnContext ctx) {
    if (ctx.asn != null) {
      return toLong(ctx.asn);
    }
    String[] parts = ctx.asn4b.getText().split("\\.");
    return (Long.parseLong(parts[0]) << 16) + Long.parseLong(parts[1]);
  }
  private static long toLong(Uint16Context ctx) {
    return Long.parseLong(ctx.getText());
  }

  private static long toLong(Uint32Context ctx) {
    return Long.parseLong(ctx.getText());
  }


  private static long toLong(DecContext ctx) {
    return Long.parseLong(ctx.getText());
  }

  private static SubRange toSubRange(SubrangeContext ctx) {
    int low = toInteger(ctx.low);
    if (ctx.DASH() != null) {
      int high = toInteger(ctx.high);
      return new SubRange(low, high);
    } else {
      return SubRange.singleton(low);
    }
  }

  private static List<SubRange> toRange(RangeContext ctx) {
    List<SubRange> range = new ArrayList<>();
    for (SubrangeContext sc : ctx.range_list) {
      SubRange sr = toSubRange(sc);
      range.add(sr);
    }
    return range;
  }
  private static int toInteger(Uint8Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(DecContext ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(Token t) {
    return Integer.parseInt(t.getText());
  }

  private static int toInteger(Vlan_idContext ctx) {
    return Integer.parseInt(ctx.getText(), 10);
  }
  private static String toInterfaceName(Interface_nameContext ctx) {
    StringBuilder name =
        new StringBuilder(
            RgosConfiguration.getCanonicalInterfaceNamePrefix(ctx.name_prefix_alpha.getText()));
    for (Token part : ctx.name_middle_parts) {
      name.append(part.getText());
    }
    if (ctx.range().range_list.size() != 1) {
      throw new BatfishException(
          "got interface range where single interface was expected: '" + ctx.getText() + "'");
    }
    name.append(ctx.range().getText());
    return name.toString();
  }


  private final @Nonnull RgosCombinedParser _parser;
  private final @Nonnull String _text;
  private final @Nonnull RgosConfiguration _configuration;
  private final @Nonnull Warnings _w;
  private final @Nonnull SilentSyntaxCollection _silentSyntax;
  private final @Nonnull BgpPeerGroup _dummyPeerGroup = new MasterBgpPeerGroup();

  private Ipv6BgpPeerGroup _currentIpv6PeerGroup;
  private NamedBgpPeerGroup _currentNamedPeerGroup;

  private BgpPeerGroup _currentPeerGroup;
  private boolean _inIpv6BgpPeer;
  private final List<BgpPeerGroup> _peerGroupStack;
  private String _currentVrf;
  private IpBgpPeerGroup _currentIpPeerGroup;
  private OspfProcess _currentOspfProcess;


  private List<Interface> _currentInterfaces;

  private static final IntegerSpace HOSTNAME_LENGTH_RANGE = IntegerSpace.of(Range.closed(1, 32));
  private static final IntegerSpace VLAN_NUMBER_RANGE = IntegerSpace.of(Range.closed(1, 4094));
  private static final Pattern HOSTNAME_PATTERN =
      Pattern.compile("[-A-Za-z0-9]+(\\.[-A-Za-z0-9]+)*");
}
