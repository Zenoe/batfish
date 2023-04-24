parser grammar RgosParser;

import
Rgos_common,
Rgos_static_routes,
Rgos_system;

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
  s_line
  | s_log_null
  | s_static_routes
  | s_system
  | s_version
;

s_line
:
  LINE VTY NEWLINE
;

s_log_null
:
  LOG SYSLOG NEWLINE
;
