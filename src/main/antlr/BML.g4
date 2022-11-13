grammar BML;

import Literals;

/*
 * Parser Rules
 */
program : bot EOF ;

bot : 'Bot' '(' elementValuePairList ')' botBody ;

elementValuePairList : elementValuePair (',' elementValuePair)* ;

elementValuePair : Identifier '=' elementValue ;

elementValue : StringLiteral
               | IntegerLiteral ;

botBody : '{' botBodyDeclaration* '}' ;

botBodyDeclaration : eventListenerDeclaration
                     | componentDeclaration ; // Semantic rule: ComponentDeclaration should only appear once in body

eventListenerDeclaration : '@' eventListenerType eventListenerHead eventListenerBody ;

eventListenerType : Identifier ('(' elementValuePairList? ')')? ;

eventListenerHead : Identifier '(' Identifier ')' ;

eventListenerBody : '{' '}' ;

componentDeclaration : 'Components' '{'  '}' ;

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
