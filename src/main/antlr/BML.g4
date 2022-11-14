grammar BML;

import Literals;

/*
 * Parser Rules
 */
program : bot EOF ;

bot : 'Bot' '(' elementValuePairList ')' botBody ;

elementValuePairList : elementValuePair (',' elementValuePair)* ;

elementValuePair : Identifier '=' elementValue ;

elementValue : literal ;

literal : StringLiteral | IntegerLiteral | FloatingPointLiteral | BooleanLiteral ;

botBody : '{' botBodyDeclaration* '}' ;

botBodyDeclaration : eventListenerDeclaration
                   | componentDeclaration ; // Semantic rule: ComponentDeclaration should only appear once in body

/*
 * Event listener declaration
 */
eventListenerDeclaration : '@' eventListenerType eventListenerHead eventListenerBody ;

eventListenerType : Identifier ('(' elementValuePairList? ')')? ;

eventListenerHead : eventListenerName '(' Identifier ')' ;

eventListenerName : Identifier ;

eventListenerBody : block ;

/*
 * Statement blocks
 */
block : '{' statement* '}' ;

statement : statementWithoutTrailingSubstatement
         | ifThenStatement 
         | ifThenElseStatement 
         | forEachStatement ;

statementWithoutTrailingSubstatement : block
                                     | assignment
                                     | functionInvocation ;

statementNoShortIf : statementWithoutTrailingSubstatement
	               | ifThenElseStatementNoShortIf
	               | forStatementNoShortIf ;

// Assignment
assignment : leftHandSide assignmentOperator rightHandSide ;

leftHandSide : Identifier ;

assignmentOperator : '='
                   | '*='
                   | '/='
                   | '%='
                   | '+='
                   | '-=' ;

rightHandSide : expression
              | functionInvocation ;

// Function invocation (TODO: what are the return values?)
functionInvocation : (functionName | objectFunction) '(' elementExpressionPairList? ')' ;

elementExpressionPairList : elementExpressionPair (',' elementExpressionPair)* ;

elementExpressionPair : Identifier '=' expression ;

functionName : Identifier ;

objectFunction : Identifier '.' functionName ;

// If Statement
ifThenStatement : 'if' expression statement ;

ifThenElseStatement : 'if' expression statementNoShortIf 'else' statement ;

ifThenElseStatementNoShortIf : 'if' expression statementNoShortIf 'else' statementNoShortIf ;

// ForEach Statement
forEachStatement : 'forEach' forEachVariable 'in' forEachDomain statement ;

forStatementNoShortIf : 'forEach' forEachVariable 'in' forEachDomain statementNoShortIf ;

forEachVariable : Identifier | (Identifier ',' Identifier) ;

forEachDomain : Identifier | domainExpression ;

domainExpression : '[' domainLimit ',' domainLimit ']' ;

domainLimit : Identifier | IntegerLiteral ;

/*
 * Expressions
 */
expression : conditionalExpression ;

conditionalExpression : conditionalOrExpression
	                  | conditionalOrExpression '?' expression ':' conditionalExpression ;

conditionalOrExpression : conditionalAndExpression
	                    | conditionalOrExpression '||' conditionalAndExpression ;

conditionalAndExpression : equalityExpression
                         | conditionalAndExpression '&&' equalityExpression ;

equalityExpression : relationalExpression
	               | equalityExpression '==' relationalExpression
	               | equalityExpression '!=' relationalExpression ;

relationalExpression : additiveExpression
                     | relationalExpression '<' additiveExpression
                     | relationalExpression '>' additiveExpression
                     | relationalExpression '<=' additiveExpression
                     | relationalExpression '>=' additiveExpression ;

additiveExpression : multiplicativeExpression
	               | additiveExpression '+' multiplicativeExpression
	               | additiveExpression '-' multiplicativeExpression ;

multiplicativeExpression : unaryPlusMinusExpression
                         | multiplicativeExpression '*' unaryPlusMinusExpression
                         | multiplicativeExpression '/' unaryPlusMinusExpression
                         | multiplicativeExpression '%' unaryPlusMinusExpression ;

unaryPlusMinusExpression : '+' unaryPlusMinusExpression
	            | '-' unaryPlusMinusExpression
	            | unaryNegationExpression ;

unaryNegationExpression : atomExpression | '!' unaryNegationExpression ;

atomExpression : Identifier
               | '(' expression ')'
               | literal
               | objectAccess ;

// Object attribute access (type is usually complex, e.g., JSON object, table, etc.)
objectAccess : Identifier ('.' Identifier)* ;

/*
 * Component list
 */
componentDeclaration : 'Components' componentBody ;

componentBody : '{' component* '}' ;

component : componentType Identifier '(' elementValuePairList? ')' ;

componentType : Identifier ;

/*
 * Lexer Rules
 */
// Keywords
BOT : 'Bot' ;
COMPONENTS : 'Components' ;
IF : 'if' ;
ELSE : 'else' ;
FOREACH : 'forEach' ;
IN : 'in' ;

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