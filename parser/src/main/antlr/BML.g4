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

botHead : BOT name=Identifier? LPAREN params=elementExpressionPairList? RPAREN ;

botBody : LBRACE (functionDefinition | component | dialogueAutomaton)* RBRACE ;

/*
 * Parameter Lists
 */
elementExpressionPairList : elementExpressionPair (COMMA elementExpressionPair)* COMMA? ;

elementExpressionPair : name=Identifier ASSIGN expr=expression ;

/*
 * Components
 */
component returns [Type type] : typeName=Identifier name=Identifier LPAREN params=elementExpressionPairList? RPAREN ;

/*
 * Function Definition (Event Listener or Action)
 */
functionDefinition returns [Scope scope, List<String> annotations] : annotation+ head=functionHead body=block ;

annotation returns [Type type] : AT name=Identifier (LPAREN params=elementExpressionPairList? RPAREN)? ;

functionHead : functionName=Identifier LPAREN parameterName=Identifier RPAREN ;

/*
 * Statement blocks
 */
block : LBRACE statement* RBRACE ;

statement returns [Scope scope] : block
                                | ifStatement
                                | forEachStatement
                                | op=BREAK
                                | expr=expression
                                | assignment ;

ifStatement : IF expr=expression thenStmt=statement (ELSE elseStmt=statement)? ;

forEachStatement : FOREACH (Identifier (comma=COMMA Identifier)?) IN expr=expression forEachBody ;

forEachBody : statement ;

assignment returns [boolean isReassignment] : name=Identifier op=(ASSIGN | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN | ADD_ASSIGN | SUB_ASSIGN) expr=expression ;

/*
 * Expressions
 */
expression returns [Type type] : op=LPAREN expr=expression RPAREN
                               | functionCall
                               | atom
                               | expr=expression op=DOT (functionCall | Identifier)
                               | expr=expression op=LBRACK index=expression RBRACK
                               | (mapInitializer | listInitializer)
                               | op=BANG expr=expression
                               | op=(SUB | ADD) expr=expression
                               | left=expression op=(MUL | DIV | MOD) right=expression
                               | left=expression op=(SUB | ADD) right=expression
                               | left=expression op=(LT | LE | GT | GT) right=expression
                               | left=expression op=(EQUAL | NOTEQUAL) right=expression
                               | left=expression op=AND right=expression
                               | left=expression op=OR right=expression
                               | <assoc=right> cond=expression op=QUESTION thenExpr=expression COLON elseExpr=expression ;

functionCall returns [Type type] : functionName=Identifier LPAREN params=elementExpressionPairList? RPAREN ;

atom returns [Type type] : token=IntegerLiteral
                         | token=FloatingPointLiteral
                         | token=StringLiteral
                         | token=BooleanLiteral
                         | token=Identifier ;

mapInitializer returns [Type type] : LBRACE params=elementExpressionPairList? RBRACE ;

listInitializer returns [Type type] : LBRACK (expression (COMMA expression)*)? RBRACK ;
