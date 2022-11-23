grammar BML;

import Literals;

@header {
    package generatedParser;

    import org.antlr.symtab.*;
}

/*
 * Parser Rules
 */
program : botDeclaration EOF ;

botDeclaration returns [Scope scope] : head=botHead body=botBody ;

botHead : 'Bot' '(' elementValuePairList ')' ;

elementValuePairList : elementValuePair (',' elementValuePair)* ;

elementValuePair : name=Identifier '=' value=elementValue ;

elementValue : literal ;

literal returns [Type type] : StringLiteral | IntegerLiteral | FloatingPointLiteral | BooleanLiteral ;

botBody : '{' (eventListenerDeclaration | component)* '}' ;

/*
 * Components
 */
component : type=componentType name=Identifier '(' params=elementValuePairList? ')' ;

componentType : Identifier ;

/*
 * Event listener declaration
 */
eventListenerDeclaration returns [Scope scope] : '@' type=eventListenerType head=eventListenerHead body=block ;

eventListenerType : typeString=Identifier ('(' elementValuePairList? ')')? ;

eventListenerHead : listenerName=Identifier '(' parameterName=Identifier ')' ;

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
assignment : name=Identifier op=assignmentOperator expression ;

assignmentOperator : '='
                   | '*='
                   | '/='
                   | '%='
                   | '+='
                   | '-=' ;

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
expression returns [Type type] : op='(' expr=expression ')'
           | op='!' expr=expression
           | op=('-' | '+') expr=expression
           | left=expression op=('*' | '/' | '%') right=expression
           | left=expression op=('+' | '-') right=expression
           | left=expression op=('<' | '<=' | '>' | '>=') right=expression
           | left=expression op=('==' | '!=') right=expression
           | left=expression op='and' right=expression
           | left=expression op='or' right=expression
           | expression op='?' expression ':' expression
           | atom ;

atom returns [Type type] : literal
                         | Identifier
                         | objectAccess
                         | functionInvocation ;

// Object attribute access (type is usually complex, e.g., JSON object, table, etc.)
objectAccess : object=Identifier ('.' Identifier)+ ;

// Function invocation
functionInvocation returns [Type type] : object=Identifier '.' functionName=Identifier  '(' elementExpressionPairList? ')' ;

elementExpressionPairList : elementExpressionPair (',' elementExpressionPair)* ;

elementExpressionPair : name=Identifier '=' expr=expression ;

/*
 * Lexer Rules
 */
// Keywords
BOT : 'Bot' ;
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