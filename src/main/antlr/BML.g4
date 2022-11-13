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

literal : StringLiteral | IntegerLiteral | FloatingPointLiteral ;

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
block : '{' blockStatement* '}' ;

blockStatement : localVariableDeclaration
               | statement ;

localVariableDeclaration : localVariableName '='  localVariableInitializer ;

localVariableName : Identifier ;

localVariableInitializer : expression ;

statement : statementWithoutTrailingSubstatement
         | ifThenStatement // done
         | ifThenElseStatement // done
         | whileStatement // done
         | forEachStatement ; // done

statementWithoutTrailingSubstatement : block
                                     | statementExpression ;

ifThenStatement : 'if' expression statement ;

ifThenElseStatement : 'if' expression statementNoShortIf 'else' statement ;

ifThenElseStatementNoShortIf : 'if' expression statementNoShortIf 'else' statementNoShortIf ;

statementNoShortIf : statementWithoutTrailingSubstatement
	               | ifThenElseStatementNoShortIf
	               | whileStatementNoShortIf
	               | forStatementNoShortIf ;

whileStatement : 'while' expression statement ;

whileStatementNoShortIf : 'while' expression statementNoShortIf ;

forEachStatement : 'forEach' forEachVariable 'in' forEachDomain statement ;

forStatementNoShortIf : 'forEach' forEachVariable 'in' forEachDomain statementNoShortIf ;

forEachVariable : Identifier | ('(' Identifier ',' Identifier ')') ;

forEachDomain : Identifier | domainExpression ;

domainExpression : '[' domainLimit ',' domainLimit ']' ;

domainLimit : Identifier | IntegerLiteral ;

expression : literal
           | Identifier
           | objectAccess
           | unary
           | expression '==' expression
           | expression '!=' expression
           | expression '<' expression
           | expression '<=' expression
           | expression '>' expression
           | expression '>=' expression
           | expression '+' expression
           | expression '-' expression
           | expression '*' expression
           | expression '/' expression
           | expression '%'  expression
           | grouping ;

objectAccess : Identifier ('.' Identifier)* ;

unary : ('-' | '+' | '!') expression ;

grouping : '(' expression ')' ;

statementExpression : assignment
                    | prefixExpression
                    | postfixExpression
                    | functionInvocation;

assignment : leftHandSide assignmentOperator rightHandSide ;

leftHandSide : Identifier ;

assignmentOperator : '='
                   | '*='
                   | '/='
                   | '%='
                   | '+='
                   | '-=' ;

rightHandSide : prefixExpression
              | postfixExpression
              | functionInvocation ;

prefixExpression : ('++' | '--') Identifier ;

postfixExpression : Identifier ('++' | '--')? ;

functionInvocation : (functionName | objectFunction) '(' elementExpressionPairList? ')' ;

functionName : Identifier ;

objectFunction : Identifier '.' functionName ;

elementExpressionPairList : elementExpressionPair (',' elementExpressionPair)* ;

elementExpressionPair : Identifier '=' expression ;

/*
 * Component list
 */
componentDeclaration : 'Components' componentBody ;

componentBody : '{' component* '}' ;

component : componentType Identifier '(' elementValuePairList? ')';

componentType : Identifier ;

/*
 * Lexer Rules
 */
// Keywords
BOT : 'Bot' ;
COMPONENTS : 'Components' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
FOREACH : 'forEach' ;

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