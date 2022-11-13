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

/*
 * Event listener declaration
 */
eventListenerDeclaration : '@' eventListenerType eventListenerHead eventListenerBody ;

eventListenerType : Identifier ('(' elementValuePairList? ')')? ;

eventListenerHead : Identifier '(' Identifier ')' ;

eventListenerBody : '{' '}' ;

/*
 * Component list
 */
componentDeclaration : 'Components' '{' componentBody '}' ;

componentBody : component* ;

component : componentType Identifier '(' elementValuePairList? ')';

componentType : Identifier ;

/*
 * Lexer Rules
 */
// Keywords
BOT : 'Bot' ;
COMPONENTS : 'Components' ;

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
TILDE : '~' ;
QUESTION : '?' ;
COLON : ':' ;
ARROW : '->' ;
EQUAL : '==' ;
LE : '<=' ;
GE : '>=' ;
NOTEQUAL : '!=' ;
AND : '&&' ;
OR : '||' ;
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
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
WHITESPACE : (' ' | '\t')+ -> skip ;
NEWLINE : ('\r'? '\n' | '\r')+ -> skip ;
SEMICOLON : ';' -> skip ;

Identifier : [a-zA-Z]+ ;
