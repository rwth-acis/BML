grammar DialogueAutomaton;

import BML;

/*
 * Dialogue
 */
dialogueAutomaton returns [Scope scope] : head=dialogueHead body=dialogueBody ;

dialogueHead : typeName=DIALOGUE name=Identifier LPAREN params=elementExpressionPairList? RPAREN ;

dialogueBody : LBRACE (dialogueFunctionDefinition | dialogueAssignment | dialogueStateCreation | dialogueTransition)* RBRACE ;

dialogueFunctionDefinition : functionDefinition ;

dialogueAssignment : assignment ;

dialogueStateCreation : functionCall ;

dialogueTransition : (functionCall | Identifier) (ARROW (functionCall | dialogueTransitionList | Identifier))+ ;

dialogueTransitionList : LBRACK ((dialogueTransition | functionCall) (COMMA (dialogueTransition | functionCall))*)? RBRACK ;