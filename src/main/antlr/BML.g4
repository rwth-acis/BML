grammar BML;

import Literals, DialogAutomaton;

@header {
    package generatedParser;

    import org.antlr.symtab.*;
}

/*
 * Parser Rules
 */
program : botDeclaration EOF ;

botDeclaration returns [Scope scope] : head=botHead body=botBody ;

botHead : BOT LPAREN elementValuePairList RPAREN ;

elementValuePairList : elementValuePair (COMMA elementValuePair)* ;

elementValuePair : name=Identifier ASSIGN value=elementValue ;

elementValue : literal ;

literal returns [Type type] : StringLiteral
                            | IntegerLiteral
                            | FloatingPointLiteral
                            | BooleanLiteral ;

botBody : LBRACE (eventListenerDeclaration | component)* RBRACE ;

/*
 * Components
 */
component : type=componentType name=Identifier LPAREN params=elementValuePairList? RPAREN ;

componentType : Identifier ;

/*
 * Event listener declaration
 */
eventListenerDeclaration returns [Scope scope] : AT type=eventListenerType head=eventListenerHead body=block ;

eventListenerType : typeString=Identifier (LPAREN elementValuePairList? RPAREN)? ;

eventListenerHead : listenerName=Identifier LPAREN parameterName=Identifier RPAREN ;

/*
 * Statement blocks
 */
block : LBRACE blockStatement* RBRACE ;

blockStatement : assignment
               | statement ;

// Assignment
assignment : name=Identifier op=assignmentOperator expression ;

assignmentOperator : ASSIGN
                   | MUL_ASSIGN
                   | DIV_ASSIGN
                   | MOD_ASSIGN
                   | ADD_ASSIGN
                   | SUB_ASSIGN ;

statement : block
          | IF expression statement (ELSE statement)?
          | FOREACH forEachVariable IN forEachDomain statement
          | statementExpression=expression ;

// ForEach Statement
forEachVariable : Identifier
                | (Identifier COMMA Identifier) ;

forEachDomain : Identifier
              | domainExpression ;

domainExpression : LBRACK domainLimit COMMA domainLimit RBRACK ;

domainLimit : Identifier
            | IntegerLiteral ;

/*
 * Expressions
 */
expression returns [Type type] : op=LPAREN expr=expression RPAREN
                               | atom
                               | expr=expression op=DOT (Identifier | functionCall)
                               | expr=expression op=LBRACE expression RBRACE
                               | functionCall
                               | op=BANG expr=expression
                               | op=(SUB | ADD) expr=expression
                               | left=expression op=(MUL | DIV | MOD) right=expression
                               | left=expression op=(SUB | ADD) right=expression
                               | left=expression op=(LT | LE | GT | GT) right=expression
                               | left=expression op=(EQUAL | NOTEQUAL) right=expression
                               | left=expression op=AND right=expression
                               | left=expression op=OR right=expression
                               | <assoc=right> expression op=QUESTION expression COLON expression ;

atom returns [Type type] : literal
                         | Identifier ;

functionCall : functionName=Identifier LPAREN elementExpressionPairList? RPAREN ;

elementExpressionPairList : elementExpressionPair (COMMA elementExpressionPair)* ;

elementExpressionPair : name=Identifier ASSIGN expr=expression ;

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
AND : 'and' ;
OR : 'or' ;
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