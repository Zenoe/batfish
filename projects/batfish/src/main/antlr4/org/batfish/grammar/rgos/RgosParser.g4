parser grammar RgosParser;

import
Rgos_system,
Rgos_bgp,
Rgos_common;

options {
   superClass = 'org.batfish.grammar.rgos.parsing.RgosBaseParser';
   tokenVocab = RgosLexer;
}

rgos_configuration
:
  NEWLINE?
  statement+ EOF
;


statement
:
  s_version
;
