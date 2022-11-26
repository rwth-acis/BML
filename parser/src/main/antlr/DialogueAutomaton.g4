grammar DialogueAutomaton;

import BML;

/*
 * Dialogue
 */
dialogueAutomaton returns [Scope scope] : head=dialogueHead body=dialogueBody ;

dialogueHead : DIALOGUE name=Identifier LPAREN elementValuePairList RPAREN ;

dialogueBody : LBRACE (functionDefinition | assignment | automatonTransitions)* RBRACE ;

automatonTransitions : . ;