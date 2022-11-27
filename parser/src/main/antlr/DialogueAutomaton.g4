grammar DialogueAutomaton;

import BML;

/*
 * Dialogue
 */
dialogueAutomaton returns [Scope scope] : head=dialogueHead body=dialogueBody ;

dialogueHead : DIALOGUE name=Identifier LPAREN elementExpressionPairList RPAREN ;

dialogueBody : LBRACE (functionDefinition | assignment | automatonTransitions)* RBRACE ;

automatonTransitions : (functionCall | Identifier) (ARROW (functionCall | transitionInitializer | Identifier))* ;

transitionInitializer : LBRACK ((expression | automatonTransitions) (COMMA (expression | automatonTransitions))*)? RBRACK ;