grammar BML;

import Tokens, Literals, DialogueAutomaton;

@header {
    package generatedParser;

    import org.antlr.symtab.*;
}

program : botDeclaration EOF ;

botDeclaration returns [Scope scope] : head=botHead body=botBody ;

botHead : BOT name=Identifier? LPAREN elementExpressionPairList RPAREN ;

elementExpressionPairList : elementExpressionPair (COMMA elementExpressionPair)* ;

elementExpressionPair : name=Identifier ASSIGN expr=expression ;

literal returns [Type type] : StringLiteral
                            | IntegerLiteral
                            | FloatingPointLiteral
                            | BooleanLiteral ;

botBody : LBRACE (functionDefinition | component | dialogueAutomaton)* RBRACE ;

/*
 * Components
 */
component : typeName=Identifier name=Identifier LPAREN params=elementExpressionPairList? RPAREN ;

/*
 * Function Definition (Event Listener or Action)
 */
functionDefinition returns [Scope scope] : AT annotation head=functionHead body=block ;

annotation : typeName=Identifier (LPAREN elementExpressionPairList? RPAREN)? ;

functionHead : functionName=Identifier LPAREN parameterName=Identifier RPAREN ;

/*
 * Statement blocks
 */
block returns [Scope scope] : LBRACE blockStatement* RBRACE ;

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
          | IF expression (statement | expression) (ELSE (statement | expression))?
          | FOREACH forEachVariable IN forEachDomain (statement | expression) ;

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
                               | expr=expression op=LBRACK expression RBRACK
                               | functionCall
                               | initializer
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

initializer : mapInitializer
            | listInitializer ;

mapInitializer : LBRACE elementExpressionPairList? RBRACE ;

listInitializer : LBRACK (expression (COMMA expression)*)? RBRACK ;