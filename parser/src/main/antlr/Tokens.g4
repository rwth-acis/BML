lexer grammar Tokens;

/*
 * Integer literal
 */
IntegerLiteral : '0'
               | NonZeroDigit (Digits? | Underscores Digits) ;

fragment Digits : Digit (DigitsAndUnderscores? Digit)? ;

fragment Digit : '0'
               | NonZeroDigit ;

fragment NonZeroDigit : [1-9] ;

fragment DigitsAndUnderscores :	DigitOrUnderscore+ ;

fragment DigitOrUnderscore : Digit
                           | '_' ;

fragment Underscores : '_'+ ;

/*
 * Floating literal
 */
FloatingPointLiteral : DecimalFloatingPointLiteral ;

fragment DecimalFloatingPointLiteral : Digits '.' Digits? ExponentPart? FloatTypeSuffix?
	                                 | '.' Digits ExponentPart? FloatTypeSuffix?
                                     | Digits ExponentPart FloatTypeSuffix?
                                     | Digits FloatTypeSuffix ;

fragment ExponentPart : ExponentIndicator SignedInteger ;

fragment ExponentIndicator : [eE] ;

fragment SignedInteger : Sign? Digits ;

fragment Sign : [+-] ;

fragment FloatTypeSuffix : [fFdD] ;

/*
 * String literal
 */
StringLiteral :	'"' StringCharacters? '"' ;

fragment StringCharacters : StringCharacter+ ;

fragment StringCharacter : ~["\\\r\n]
                         | EscapeSequence ;

fragment EscapeSequence : '\\' [btnfr"'\\]
	                    | OctalEscape
                        | UnicodeEscape ;

fragment OctalEscape : '\\' OctalDigit
	                 | '\\' OctalDigit OctalDigit
	                 | '\\' ZeroToThree OctalDigit OctalDigit ;

fragment ZeroToThree : [0-3] ;

fragment UnicodeEscape : '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit ;

fragment HexDigit : [0-9a-fA-F] ;

fragment OctalDigit : [0-7] ;

/*
 * Boolean literal
 */
BooleanLiteral : 'true'
	           | 'false' ;

// Keywords
BOT : 'Bot' ;
DIALOGUE : 'Dialogue' ;
IF : 'if' ;
ELSE : 'else' ;
FOREACH : 'forEach' ;
IN : 'in' ;
BREAK: 'break' ;

// Separators
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
LBRACK : '[' ;
RBRACK : ']' ;
DOT : '.' ;
COMMA : ',' ;
AT : '@' ;

// Operators
ASSIGN : '=' ;
GT : '>' ;
LT : '<' ;
BANG : '!' ;
QUESTION : '?' ;
COLON : ':' ;
ARROW : '->' ;
EQUAL : '==' ;
LE : '<=' ;
GE : '>=' ;
NOTEQUAL : '!=' ;
AND : ('and' | '&&') ;
OR : ('or' | '||') ;
INC : '++' ;
DEC : '--' ;
ADD : '+' ;
SUB : '-' ;
MUL : '*' ;
DIV : '/' ;
MOD : '%' ;
ADD_ASSIGN : '+=' ;
SUB_ASSIGN : '-=' ;
MUL_ASSIGN : '*=' ;
DIV_ASSIGN : '/=' ;
MOD_ASSIGN : '%=' ;

// IGNORED
COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;
LINE_COMMENT : '//' ~[\r\n]* -> channel(HIDDEN) ;
WHITESPACE : (' ' | '\t')+ -> channel(HIDDEN) ;
NEWLINE : ('\r'? '\n' | '\r')+ -> channel(HIDDEN) ;
SEMICOLON : ';' -> channel(HIDDEN) ;

// Identifier needs to come last
Identifier : [a-zA-Z$_] ([a-zA-Z$_] | [0-9])*;