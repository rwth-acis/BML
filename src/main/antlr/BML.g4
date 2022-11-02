grammar BML;

import Literals;

/*
 * Parser Rules
 */
bot : 'Bot' parameterList botBody ;

parameterList : '(' parameter (',' parameter)* ')';
parameter : Identifier '=' (StringLiteral | IntegerLiteral) ;

botBody : '{' botBodyDeclaration* '}' ;

botBodyDeclaration : . ;

/*
 * Lexer Rules
 */

// BASIC TOKENS
Identifier : [a-zA-Z]+ ;

// IGNORED
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
WHITESPACE : (' ' | '\t')+ -> skip ;
NEWLINE : ('\r'? '\n' | '\r')+ -> skip ;
