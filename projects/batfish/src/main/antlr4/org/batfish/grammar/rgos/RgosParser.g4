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

domain_lookup
:
   LOOKUP
   (
      SOURCE_INTERFACE iname = interface_name
      | DISABLE
   ) NEWLINE
;

domain_name
:
   NAME hostname = variable_hostname NEWLINE
;

domain_name_server
:
   NAME_SERVER hostname = variable_hostname NEWLINE
;

s_domain
:
   DOMAIN
   (
      VRF vrf = variable
   )?
   (
      domain_lookup
      | domain_name
      | domain_name_server
   )
;


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
   | s_domain
   | s_version
   | router_bgp_stanza

;
