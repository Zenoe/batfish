parser grammar Rgos_bgp;

import Rgos_common;

options {
   tokenVocab = RgosLexer;
}

s_version
:
   (
      VERSION
   )
   (
      quoted_name = double_quoted_string
      |
      (
         (
            name_parts += ~NEWLINE
         )+
      )
   ) NEWLINE
;
