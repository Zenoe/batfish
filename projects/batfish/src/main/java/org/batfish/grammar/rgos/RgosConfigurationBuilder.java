/*
 *This class could be responsible for building a Batfish-specific configuration representation or data structure
 *from the parsed configuration file using the parse tree generated by the "RgosCombinedParser.java" class.
 */
package org.batfish.grammar.rgos;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSortedSet;


import com.google.common.collect.Range;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.SubRange;
import static org.batfish.representation.rgos.RgosStructureType.INTERFACE;
import static org.batfish.representation.rgos.RgosStructureUsage.INTERFACE_SELF_REF;

// import org.batfish.datamodel.Ip;
// import org.batfish.datamodel.Prefix;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.SilentSyntaxListener;
import org.batfish.grammar.UnrecognizedLineToken;

import org.batfish.common.BatfishException;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

import org.batfish.grammar.rgos.RgosParser.S_interface_definitionContext;
import org.batfish.grammar.rgos.RgosParser.S_interface_definitionContext;
import org.batfish.grammar.rgos.RgosParser.Interface_nameContext;

import org.batfish.representation.rgos.Interface;

// import org.batfish.grammar.rgos.RgosParser.Host_nameContext;
// import org.batfish.grammar.rgos.RgosParser.Interface_nameContext;
// import org.batfish.grammar.rgos.RgosParser.Ipv4_addressContext;
// import org.batfish.grammar.rgos.RgosParser.Ipv4_prefixContext;
// import org.batfish.grammar.rgos.RgosParser.S_lineContext;
// import org.batfish.grammar.rgos.RgosParser.Ss_addContext;
// import org.batfish.grammar.rgos.RgosParser.Ss_deleteContext;
// import org.batfish.grammar.rgos.RgosParser.Ss_disableContext;
// import org.batfish.grammar.rgos.RgosParser.Ss_enableContext;
// import org.batfish.grammar.rgos.RgosParser.Ss_modifyContext;
// import org.batfish.grammar.rgos.RgosParser.Ssa_discardContext;
// import org.batfish.grammar.rgos.RgosParser.Ssa_gatewayContext;
// import org.batfish.grammar.rgos.RgosParser.Ssa_interfaceContext;
// import org.batfish.grammar.rgos.RgosParser.Ssy_host_nameContext;
// import org.batfish.grammar.rgos.RgosParser.Rgos_versionContext;
// import org.batfish.grammar.rgos.RgosParser.StringContext;
// import org.batfish.grammar.rgos.RgosParser.Uint16Context;
// import org.batfish.grammar.rgos.RgosParser.Uint8Context;
// import org.batfish.grammar.rgos.RgosParser.Vlan_numberContext;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.representation.rgos.RgosConfiguration;
import org.batfish.representation.rgos.NextHop;
// import org.batfish.vendor.rgos.NextHopDiscard;
// import org.batfish.vendor.rgos.NextHopGateway;
// import org.batfish.vendor.rgos.NextHopInterface;
import org.batfish.representation.rgos.StaticRoute;

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

  private Interface addInterface(String name, Interface_nameContext ctx, boolean explicit) {
    Interface newInterface = _configuration.getInterfaces().get(name);
    if (newInterface == null) {
      newInterface = new Interface(name, _configuration);
      _configuration.getInterfaces().put(name, newInterface);
      initInterface(newInterface, ctx);
    } else {
      _w.pedantic("Interface: '" + name + "' altered more than once");
    }
    newInterface.setDeclaredNames(
        new ImmutableSortedSet.Builder<String>(naturalOrder())
            .addAll(newInterface.getDeclaredNames())
            .add(ctx.getText())
            .build());
    if (explicit) {
      _currentInterfaces.add(newInterface);
    }
    return newInterface;
  }


  private void initInterface(Interface iface, Interface_nameContext ctx) {
    String nameAlpha = ctx.name_prefix_alpha.getText();
    String canonicalNamePrefix = RgosConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    String vrf =
        canonicalNamePrefix.equals(RgosConfiguration.MANAGEMENT_INTERFACE_PREFIX)
            ? RgosConfiguration.MANAGEMENT_VRF_NAME
            : Configuration.DEFAULT_VRF_NAME;
    int mtu = Interface.getDefaultMtu();
    iface.setVrf(vrf);
    initVrf(vrf);
    iface.setMtu(mtu);
  }

  private final @Nonnull RgosCombinedParser _parser;
  private final @Nonnull String _text;
  private final @Nonnull RgosConfiguration _configuration;
  private final @Nonnull Warnings _w;
  private final @Nonnull SilentSyntaxCollection _silentSyntax;

  private StaticRoute _currentStaticRoute;
  private NextHop _currentNextHop;
  private List<Interface> _currentInterfaces;

  private static final IntegerSpace HOSTNAME_LENGTH_RANGE = IntegerSpace.of(Range.closed(1, 32));
  private static final IntegerSpace VLAN_NUMBER_RANGE = IntegerSpace.of(Range.closed(1, 4094));
  private static final Pattern HOSTNAME_PATTERN =
      Pattern.compile("[-A-Za-z0-9]+(\\.[-A-Za-z0-9]+)*");
}
