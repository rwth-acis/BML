lexer grammar Literals;

/*
 * Integer literal
 */
IntegerLiteral : '0' | (NonZeroDigit (Digits? | Underscores Digits)) ;

fragment Digits : Digit (DigitsAndUnderscores? Digit)? ;

fragment Digit : '0' | NonZeroDigit ;

fragment NonZeroDigit : [1-9] ;

fragment DigitsAndUnderscores :	DigitOrUnderscore+ ;

fragment DigitOrUnderscore : Digit | '_' ;

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
 * String literal
 */
BooleanLiteral : 'true'
	           | 'false' ;