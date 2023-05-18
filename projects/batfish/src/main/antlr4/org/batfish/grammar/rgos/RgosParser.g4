parser grammar RgosParser;

import
Rgos_system,
Rgos_bgp,
Rgos_interface,
Rgos_common;

options {
   superClass = 'org.batfish.grammar.rgos.parsing.RgosBaseParser';
   tokenVocab = RgosLexer;
}

rgos_configuration
:
   NEWLINE?
   (sl += stanza)+
   COLON? NEWLINE?
   EOF
;

s_interface_line
:
   NO? INTERFACE BREAKOUT null_rest_of_line
;

stanza
:
   s_hostname
   | s_interface_line
   | s_interface
   | s_version
;
