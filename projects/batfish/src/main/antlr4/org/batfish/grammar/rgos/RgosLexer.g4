lexer grammar RgosLexer;

options {
   superClass = 'org.batfish.grammar.rgos.parsing.RgosBaseLexer';
}


AAA: 'aaa';

ACTIVATE: 'activate';

ADDRESS_FAMILY: 'address-family';

VERSION: 'version';

NEWLINE
:
  F_Newline
  {
    _enableIpv6Address = true;
    _enableDec = true;
    _enableRegex = false;
    _enableAclNum = false;
    _inAccessList = false;
  }
;

WS
:
   F_Whitespace+ -> channel ( HIDDEN )
; // Fragments

VARIABLE
:
   (
      (
         F_Variable_RequiredVarChar
         (
            (
               {!_enableIpv6Address}?

               F_Variable_VarChar*
            )
            |
            (
               {_enableIpv6Address}?

               F_Variable_VarChar_Ipv6*
            )
         )
      )
      |
      (
         (
            F_Variable_VarChar
            {!_enableIpv6Address}?

            F_Variable_VarChar* F_Variable_RequiredVarChar F_Variable_VarChar*
         )
         |
         (
            F_Variable_VarChar_Ipv6
            {_enableIpv6Address}?

            F_Variable_VarChar_Ipv6* F_Variable_RequiredVarChar
            F_Variable_VarChar_Ipv6*
         )
      )
   )
   {
      if (_enableAclNum) {
         _enableAclNum = false;
         _enableDec = true;
      }
      if (_enableCommunityListNum) {
         _enableCommunityListNum = false;
         _enableDec = true;
      }
   }
;

DOUBLE_QUOTE
:
   '"'
;
PAREN_LEFT
:
   '('
;
PAREN_RIGHT
:
   ')'
;


fragment
F_Variable_RequiredVarChar
:
   ~( '0' .. '9' | '-' | [ \t\u000C\u00A0\n\r(),!+$'"*#] | '[' | ']' | [/.] | ':' )
;


fragment
F_Variable_VarChar
:
   ~( [ \t\u000C\u00A0\n\r(),!$'"*#] | '[' | ']' )
;


fragment
F_Variable_VarChar_Ipv6
:
   ~( [ \t\u000C\u00A0\n\r(),!$'"*#] | '[' | ']' | ':' )
;

fragment
F_Newline
:
  F_NewlineChar (F_Whitespace* F_NewlineChar+)*
;

// A single newline character [sequence - allowing \r, \r\n, or \n]
fragment
F_NewlineChar
:
  '\r' '\n'?
  | '\n'
;

fragment
F_NonNewline
:
   ~[\n\r]
;

fragment
F_Whitespace
:
   ' '
   | '\t'
   | '\u000C'
   | '\u00A0'
;
