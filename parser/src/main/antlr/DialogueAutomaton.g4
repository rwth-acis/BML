grammar DialogueAutomaton;

import BML;

/*
 * Dialogue
 */
dialogueAutomaton returns [Scope scope] : head=dialogueHead body=dialogueBody ;

dialogueHead : DIALOGUE name=Identifier LPAREN elementExpressionPairList RPAREN ;

dialogueBody : LBRACE (dialogueFunctionDefinition | dialogueAssignment | automatonTransitions)* RBRACE ;

dialogueFunctionDefinition : functionDefinition ;

dialogueAssignment : assignment ;

automatonTransitions : (functionCall | Identifier) (ARROW (functionCall | transitionInitializer | Identifier))* ;

transitionInitializer : LBRACK (automatonTransitions (COMMA automatonTransitions)*)? RBRACK ;