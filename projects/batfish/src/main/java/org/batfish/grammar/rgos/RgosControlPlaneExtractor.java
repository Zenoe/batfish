/*
 *This class may be responsible for extracting control plane information, such as routing tables,
 *interfaces, and other network topology details, from the parsed configuration using the Batfish
 *configuration representation built by the "RgosConfigurationBuilder.java" class.
 */
package org.batfish.grammar.rgos;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Set;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.ImplementedRules;
import org.batfish.grammar.rgos.RgosParser.Rgos_configurationContext;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.vendor.VendorConfiguration;
import org.batfish.vendor.rgos.RgosConfiguration;

/** Extracts data from RGOS parse tree into a {@link RgosConfiguration}. */
public final class RgosControlPlaneExtractor implements ControlPlaneExtractor {

  public RgosControlPlaneExtractor(
      String fileText,
      RgosCombinedParser combinedParser,
      Warnings warnings,
      SilentSyntaxCollection silentSyntax) {
    _text = fileText;
    _parser = combinedParser;
    _w = warnings;
    _silentSyntax = silentSyntax;
  }

  @Override
  public Set<String> implementedRuleNames() {
    return ImplementedRules.getImplementedRules(RgosConfigurationBuilder.class);
  }

  @Override
  public VendorConfiguration getVendorConfiguration() {
    return _configuration;
  }

  @Override
  public void processParseTree(NetworkSnapshot snapshot, ParserRuleContext tree) {
    checkArgument(
        tree instanceof Rgos_configurationContext,
        "Expected %s, not %s",
        Rgos_configurationContext.class,
        tree.getClass());
    // TOOD: insert any pre-processing of the parse tree here

    // Build configuration from pre-processed parse tree
    RgosConfigurationBuilder cb =
        new RgosConfigurationBuilder(_parser, _text, _w, _silentSyntax);
    new BatfishParseTreeWalker(_parser).walk(cb, tree);
    _configuration = cb.getConfiguration();
  }

  private RgosConfiguration _configuration;
  private final RgosCombinedParser _parser;
  private final String _text;
  private final Warnings _w;
  private final @Nonnull SilentSyntaxCollection _silentSyntax;
}
