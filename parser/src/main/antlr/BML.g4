grammar BML;

import Tokens, DialogueAutomaton;

@header {
    package generatedParser;

    import org.antlr.symtab.*;
}

program : botDeclaration EOF ;

/*
 * Bot declaration
 */
botDeclaration returns [Scope scope] : head=botHead body=botBody ;

botHead : BOT name=Identifier? LPAREN elementExpressionPairList RPAREN ;

botBody : LBRACE (functionDefinition | component | dialogueAutomaton)* RBRACE ;

/*
 * Parameter Lists
 */
elementExpressionPairList returns [Scope scope] : elementExpressionPair (COMMA elementExpressionPair)* COMMA? ;

elementExpressionPair : name=Identifier ASSIGN expr=expression ;

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
statement returns [Scope scope] : block
                                | ifStatement
                                | forEachStatement
                                | expr=expression
                                | assignment ;

block : LBRACE statement* RBRACE ;

ifStatement : IF expression statement (ELSE statement)? ;

forEachStatement : FOREACH (Identifier (comma=COMMA Identifier)?) IN expr=expression forEachBody ;

forEachBody : statement ;

assignment : name=Identifier op=(ASSIGN | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN | ADD_ASSIGN | SUB_ASSIGN) expr=expression ;

/*
 * Expressions
 */
expression returns [Type type] : op=LPAREN expr=expression RPAREN
                               | atom
                               | expr=expression op=DOT (Identifier | functionCall)
                               | expr=expression op=LBRACK index=expression RBRACK
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
                               | <assoc=right> cond=expression op=QUESTION thenExpr=expression COLON elseExpr=expression ;

atom returns [Type type] : token=IntegerLiteral
                         | token=FloatingPointLiteral
                         | token=StringLiteral
                         | token=BooleanLiteral
                         | token=Identifier ;

functionCall returns [Type type] : functionName=Identifier LPAREN params=elementExpressionPairList? RPAREN ;

initializer returns [Type type] : mapInitializer
            | listInitializer ;

mapInitializer returns [Type type] : LBRACE params=elementExpressionPairList? RBRACE ;

listInitializer returns [Type type] : LBRACK (expression (COMMA expression)*)? RBRACK ;
