package org.batfish.grammar.rgos;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.rgos.RgosParser.Rgos_configurationContext;

public class RgosCombinedParser extends BatfishCombinedParser<RgosParser, RgosLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(RgosLexer.NEWLINE, "\n");

  public RgosCombinedParser(String input, GrammarSettings settings) {
    super(
        RgosParser.class,
        RgosLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Rgos_configurationContext parse() {
    return _parser.rgos_configuration();
  }
}
