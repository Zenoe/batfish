parser grammar RgosParser;

import Rgos_common;

options {
   tokenVocab = RgosLexer;
}

s_hostname
:
   HOSTNAME
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
