package i5.bml.transpiler.generators.dialogue;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.dialogue.BMLState;
import i5.bml.transpiler.bot.config.BotConfig;
import i5.bml.transpiler.bot.dialogue.ActionsTemplate;
import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;
import i5.bml.transpiler.bot.dialogue.DialogueAutomatonTemplate;
import i5.bml.transpiler.bot.dialogue.State;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.generators.types.BMLTypeResolver;
import i5.bml.transpiler.utils.IOUtil;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class DialogueAutomatonGenerator {

    private int stateCounter = 1;

    private final JavaTreeGenerator javaTreeGenerator;

    private ClassOrInterfaceDeclaration dialogueClass;

    private CompilationUnit dialogueCompilationUnit;

    private ClassOrInterfaceDeclaration actionClass;

    private BlockStmt initMethodBody;

    private final String dialogueOutputPath;

    private final Map<String, BlockStmt> stateActions = new HashMap<>();

    public DialogueAutomatonGenerator(JavaTreeGenerator javaTreeGenerator) {
        this.javaTreeGenerator = javaTreeGenerator;
        dialogueOutputPath = javaTreeGenerator.botOutputPath() + "dialogue";
    }

    public void init(BMLParser.DialogueHeadContext ctx) {
        // Copy required implementation for dialogues
        IOUtil.copyDirAndRenameImports("dialogue", javaTreeGenerator);

        // Duplicate templates for DialogueAutomaton and Actions
        var newDialogueClassName = "%sDialogueAutomaton".formatted(StringUtils.capitalize(ctx.name.getText()));
        var newActionsClassName = "%sActions".formatted(StringUtils.capitalize(ctx.name.getText()));
        PrinterUtil.copyClass(javaTreeGenerator.botOutputPath() + "dialogue", DialogueAutomatonTemplate.class.getSimpleName(), newDialogueClassName);
        PrinterUtil.copyClass(javaTreeGenerator.botOutputPath() + "dialogue", ActionsTemplate.class.getSimpleName(), newActionsClassName);

        // Read freshly copied classes
        dialogueClass = PrinterUtil.readClass(dialogueOutputPath, newDialogueClassName);
        actionClass = PrinterUtil.readClass(dialogueOutputPath, newActionsClassName);
    }

    public void visitDialogueBody(BMLParser.DialogueBodyContext ctx, Scope currentScope) {
        dialogueCompilationUnit = dialogueClass.findCompilationUnit().get();
        var actionCompilationUnit = actionClass.findCompilationUnit().get();
        initMethodBody = dialogueClass.getMethodsByName("initTransitions").get(0).getBody().get();

        // Forward named states such that they are available at all points in `initTransitions`
        for (var assignmentContext : ctx.dialogueAssignment()) {
            if (assignmentContext.assignment().expr.type instanceof BMLState) {
                // First create a respective class
                visitDialogueAssignment(assignmentContext);

                // Second, we can add it to the `initTransitions` method to be instantiated and connected via transitions
                var name = assignmentContext.assignment().name.getText();
                var className = StringUtils.capitalize(name) + "State";

                // Create variable in dialogue class
                var stateVarType = StaticJavaParser.parseClassOrInterfaceType(className);
                var stateName = name + "State";
                addVariableForState(initMethodBody, stateName, stateVarType, new NodeList<>(new NameExpr("this")),
                        assignmentContext.assignment().expr.getText());

                // Add to namedStates
                initMethodBody.addStatement(new MethodCallExpr(new NameExpr("namedStates"), "put",
                        new NodeList<>(new StringLiteralExpr(name), new NameExpr(stateName))));

                // Add import for `className` to dialogue class
                dialogueCompilationUnit.addImport(javaTreeGenerator.outputPackage() + "dialogue.states." + className);

                // Add transitions depending on function type to dialogue class `initTransitions` method
                var funcCall = assignmentContext.assignment().expr.functionCall();
                if (funcCall != null && funcCall.functionName.getText().equals("initial")) {
                    addTransition(initMethodBody, "defaultState", stateName, ((BMLState) ((BMLFunctionType) funcCall.type).getReturnType()).getIntent());
                }
            }
        }

        // Iterate over child nodes in body in procedural manner, i.e., as they appear in the source text
        for (var child : ctx.children) {
            if (child.getText().equals("{") || child.getText().equals("}")) {
                continue;
            }

            if (child instanceof BMLParser.DialogueFunctionDefinitionContext functionDefinitionContext) { // Action definitions
                javaTreeGenerator.classStack().push(actionClass);

                var actionMethod = actionClass.addMethod(functionDefinitionContext.functionDefinition().head.functionName.getText(),
                        Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                actionMethod.addParameter(MessageEventContext.class.getSimpleName(), "ctx");
                actionMethod.addParameter(dialogueClass.getNameAsString(), "dialogueAutomaton");

                // Add import for `MessageEventContext`
                actionCompilationUnit.addImport(Utils.renameImport(MessageEventContext.class, javaTreeGenerator.outputPackage()), false, false);

                // We visit the whole function definition to have a scope created
                actionMethod.setBody((BlockStmt) javaTreeGenerator.visit(functionDefinitionContext.functionDefinition()));

                javaTreeGenerator.classStack().pop();
            } else { // Anything else than action definitions goes into dialogue class
                javaTreeGenerator.classStack().push(dialogueClass);

                if (child instanceof BMLParser.DialogueAssignmentContext assignmentContext) { // Assignments (Only other types, states have been forwarded)
                    if (!(assignmentContext.assignment().expr.type instanceof BMLState)) {
                        // We have some "normal" type, add field with getter to class
                        var field = dialogueClass.addFieldWithInitializer(BMLTypeResolver.resolveBMLTypeToJavaType(assignmentContext.assignment().expr.type),
                                assignmentContext.assignment().name.getText(), (Expression) javaTreeGenerator.visit(assignmentContext.assignment().expr),
                                Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
                        Utils.generateRecordStyleGetter(field, false);
                    }
                } else if (child instanceof BMLParser.DialogueStateCreationContext stateCreationContext) { // State creation, i.e., state function calls
                    var block = visitDialogueStateCreation(stateCreationContext);
                    initMethodBody.getStatements().addAll(block.getStatements());
                } else if (child instanceof BMLParser.DialogueTransitionContext transitionContext) { // Take care of dialogue transitions
                    visitDialogueTransition(transitionContext, currentScope);
                }

                javaTreeGenerator.classStack().pop();
            }
        }

        // We have to do sinks last, since we need to collect all states before
        ctx.dialogueStateCreation().stream()
                .filter(d -> d.functionCall().functionName.getText().equals("sink"))
                .forEach(d -> {
                    var stateType = (BMLState) ((BMLFunctionType) d.functionCall().type).getReturnType();

                    // Generate action for sink state
                    var actionLambdaExpr = generateLambdaExprForAction(stateType);

                    // The sink state jumps back to the default state after executing its action
                    addJumpToDefaultStateIfNotPresent(((LambdaExpr) actionLambdaExpr).getBody().asBlockStmt());

                    // Create variable for sink state
                    var sinkName = "state" + stateCounter++;
                    addVariableForStateWithoutCollection(initMethodBody, sinkName, StaticJavaParser.parseClassOrInterfaceType("State"),
                            new NodeList<>(actionLambdaExpr), d.functionCall().getText());

                    var forExprBody = new BlockStmt();
                    var intents = stateType.getIntent().split(",");
                    for (String intent : intents) {
                        var transitionExpr = new MethodCallExpr(new NameExpr("s"), "addTransition",
                                new NodeList<>(new StringLiteralExpr(intent.replaceAll(" ", "")), new NameExpr(sinkName)));
                        forExprBody.addStatement(transitionExpr);
                    }

                    var forExpr = new ForEachStmt(new VariableDeclarationExpr(new VarType(), "s"), "states", forExprBody);
                    initMethodBody.addStatement(forExpr);
                });

        // Add fallback transition to all states that not yet overwrite the fallback intent
        var fallbackIntent = new FieldAccessExpr(new NameExpr(BotConfig.class.getSimpleName()), "NLU_FALLBACK_INTENT");
        var addTransition = new MethodCallExpr(new NameExpr("s"), "addTransition", new NodeList<>(fallbackIntent, new NameExpr("defaultState")));
        var ifStmt = new IfStmt(new UnaryExpr(new MethodCallExpr(new NameExpr("s"), "hasTransition", new NodeList<>(fallbackIntent)), UnaryExpr.Operator.LOGICAL_COMPLEMENT),
                new ExpressionStmt(addTransition), null);
        var forExpr = new ForEachStmt(new VariableDeclarationExpr(new VarType(), "s"), "states", new BlockStmt().addStatement(ifStmt));
        forExpr.setComment(new LineComment("Add fallback transition to all states that not yet overwrite the fallback intent"));
        initMethodBody.addStatement(forExpr);

        // Add import for `BotConfig`
        dialogueCompilationUnit.addImport(Utils.renameImport(BotConfig.class, javaTreeGenerator.outputPackage()), false, false);

        // Finally, write back action and dialogue classes
        PrinterUtil.writeClass(dialogueOutputPath, dialogueCompilationUnit, dialogueClass);
        PrinterUtil.writeClass(dialogueOutputPath, actionCompilationUnit, actionClass);
    }

    private void addJumpToDefaultStateIfNotPresent(BlockStmt block) {
        var notContainsJumpOrActionCall = block.getStatements().stream()
                .filter(s -> s.isExpressionStmt() && s.asExpressionStmt().getExpression().isMethodCallExpr())
                .map(s -> s.asExpressionStmt().getExpression().asMethodCallExpr())
                .noneMatch(m -> {
                    return m.getNameAsString().equals("jumpTo")
                            || m.getNameAsString().equals("jumpToWithoutAction")
                            || (m.getScope().isPresent() && m.getScope().get().asNameExpr().getNameAsString().equals(actionClass.getNameAsString()));
                });

        if (notContainsJumpOrActionCall) {
            block.addStatement(new MethodCallExpr("jumpToWithoutAction", new NameExpr("defaultState")));
        }
    }

    private void visitDialogueAssignment(BMLParser.DialogueAssignmentContext ctx) {
        var stateType = (BMLState) ctx.assignment().expr.type;

        // Create a class
        CompilationUnit cu = new CompilationUnit();

        // Add package declaration
        cu.setPackageDeclaration(javaTreeGenerator.outputPackage() + "dialogue.states");

        // Set class name and extends
        var className = StringUtils.capitalize(ctx.assignment().name.getText()) + "State";
        var clazz = cu.addClass(className);
        clazz.addExtendedType(State.class.getSimpleName());

        // Make import for extends `State`
        cu.addImport(Utils.renameImport(State.class, javaTreeGenerator.outputPackage()), false, false);

        // Add field to store & set intent
        clazz.addField(DialogueAutomaton.class.getSimpleName(), "dialogueAutomaton", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        // Add import for `DialogueAutomaton`
        cu.addImport(Utils.renameImport(DialogueAutomaton.class, javaTreeGenerator.outputPackage()), false, false);

        // Add constructor
        var dialogueAutomatonField = new AssignExpr(new FieldAccessExpr(new NameExpr("this"), "dialogueAutomaton"),
                new NameExpr("dialogueAutomaton"), AssignExpr.Operator.ASSIGN);
        var constructorBody = new BlockStmt().addStatement(dialogueAutomatonField);
        clazz.addConstructor()
                .setParameters(new NodeList<>(new Parameter(StaticJavaParser.parseType(DialogueAutomaton.class.getSimpleName()), "dialogueAutomaton")))
                .setModifiers(Modifier.Keyword.PUBLIC)
                .setBody(constructorBody);

        // Set action
        var actionMethod = clazz.addMethod("action", Modifier.Keyword.PUBLIC);
        actionMethod.addMarkerAnnotation(Override.class);
        actionMethod.addParameter("MessageEventContext", "ctx");
        actionMethod.setBody(generateBlockForStateAction(stateType, cu, clazz));

        // Add import for action parameter of type `MessageEventContext`
        cu.addImport(Utils.renameImport(MessageEventContext.class, javaTreeGenerator.outputPackage()), false, false);

        // Create file at desired destination
        PrinterUtil.writeClass(javaTreeGenerator.botOutputPath() + "dialogue/states", className, cu);
    }

    private BlockStmt visitDialogueStateCreation(BMLParser.DialogueStateCreationContext ctx) {
        var pair = addAnonymousStateForFunctionCall(ctx.functionCall());
        if (!ctx.functionCall().functionName.getText().equals("default")) {
            addJumpToDefaultStateIfNotPresent(pair.getRight().getBody().asBlockStmt());
        }
        return pair.getLeft();
    }

    private void addTransition(BlockStmt block, String from, String to, String withIntent) {
        var intents = withIntent.split(",");
        for (String intent : intents) {
            intent = intent.replaceAll(" ", "");
            Expression intentExpr;
            if (intent.equals("fallback")) {
                intentExpr = new FieldAccessExpr(new NameExpr(BotConfig.class.getSimpleName()), "NLU_FALLBACK_INTENT");
                dialogueCompilationUnit.addImport(Utils.renameImport(BotConfig.class, javaTreeGenerator.outputPackage()), false, false);
            } else {
                intentExpr = new StringLiteralExpr(intent);
            }
            var transition = new MethodCallExpr(new NameExpr(from), "addTransition",
                    new NodeList<>(intentExpr, new NameExpr(to)));
            block.addStatement(transition);
        }
    }

    private void addVariableForStateWithoutCollection(BlockStmt block, String stateName, ClassOrInterfaceType stateType, NodeList<Expression> args,
                                     String commentText) {
        var stateObject = new ObjectCreationExpr(null, stateType, args);
        var stateVar = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), stateName, stateObject));
        // Add comment to indicate origin
        stateVar.setComment(new LineComment("State represents: %s".formatted(commentText)));

        block.addStatement(stateVar);
    }

    private void addVariableForState(BlockStmt block, String stateName, ClassOrInterfaceType stateType, NodeList<Expression> args,
                                     String commentText) {
        addVariableForStateWithoutCollection(block, stateName, stateType, args, commentText);
        block.addStatement(new MethodCallExpr(new NameExpr("states"), "add", new NodeList<>(new NameExpr(stateName))));
    }

    private Expression generateLambdaExprForAction(BMLState stateType) {
        // Create a lambda expression for specified action
        var block = generateBlockForStateAction(stateType, dialogueCompilationUnit, dialogueClass);
        return new LambdaExpr(new Parameter(new UnknownType(), "ctx"), block);
    }

    private BlockStmt generateBlockForStateAction(BMLState stateType, CompilationUnit cu, ClassOrInterfaceDeclaration clazz) {
        return switch (stateType.getActionType().getClass().getAnnotation(BMLType.class).name()) {
            case STRING -> {
                cu.addImport(Utils.renameImport(MessageHelper.class, javaTreeGenerator.outputPackage()), false, false);
                yield new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                        new NodeList<>(new NameExpr("ctx"), (StringLiteralExpr) javaTreeGenerator.visit(stateType.getAction()))));
            }
            case LIST -> {
                // Add import for `Random`
                cu.addImport(Utils.renameImport(Random.class, javaTreeGenerator.outputPackage()), false, false);

                var randomType = StaticJavaParser.parseClassOrInterfaceType("Random");
                clazz.addFieldWithInitializer(Random.class, "random",
                        new ObjectCreationExpr(null, randomType, new NodeList<>()), Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                var listNameExpr = (Expression) javaTreeGenerator.visit(stateType.getAction());
                var getRandomListEntry = new MethodCallExpr(listNameExpr, "get", new NodeList<>(new NameExpr("nextMessage")));

                var listSizeExpr = new MethodCallExpr(listNameExpr, "size");
                var getRandomIntStmt = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), "nextMessage",
                        new MethodCallExpr(new NameExpr("random"), "nextInt", new NodeList<>(listSizeExpr))));

                var sendMessageStmt = new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                        new NodeList<>(new NameExpr("ctx"), getRandomListEntry));

                yield new BlockStmt().addStatement(getRandomIntStmt).addStatement(sendMessageStmt);
            }
            case FUNCTION -> {
                // Add import for `<name>Actions`
                var actionsClassName = clazz.getNameAsString().replace("DialogueAutomaton", "") + "Actions";
                cu.addImport(cu.getPackageDeclaration().get().getNameAsString() + actionsClassName, false, false);

                // Add call for action
                var identifier = (NameExpr) javaTreeGenerator.visit(stateType.getAction());
                yield new BlockStmt().addStatement(new MethodCallExpr(new NameExpr(actionsClassName), identifier.getName(), new NodeList<>(new NameExpr("ctx"), new NameExpr("this"))));
            }
            case STATE -> {
                var functionType = (BMLFunctionType) stateType.getAction().functionCall().type;

                // Since we are generating the jumpTo function (that returns a state), we find the destination and then create the call expr
                var destinationState = (NameExpr) javaTreeGenerator.visit(functionType.getRequiredParameters().get(0).getExprCtx());
                destinationState.setName(destinationState.getNameAsString() + "State");
                var methodCallExpr = new MethodCallExpr(null, "jumpTo", new NodeList<>(destinationState, new NameExpr("ctx")));

                yield new BlockStmt().addStatement(methodCallExpr);
            }
            default -> new BlockStmt();
        };
    }

    public Pair<List<DialogueState>, List<DialogueState>> visitDialogueTransition(BMLParser.DialogueTransitionContext ctx, Scope currentScope) {
        List<DialogueState> prevStates = new ArrayList<>();
        List<DialogueState> startStates = new ArrayList<>();
        var transitionBlock = new BlockStmt();

        for (var child : ctx.children) {
            if (child.getText().equals("->")) {
                continue;
            }

            if (child instanceof BMLParser.DialogueTransitionListContext dialogueTransitionListContext) {
                var stateListPair = visitDialogueTransitionList(dialogueTransitionListContext, currentScope);
                var transitionStartStates = stateListPair.getLeft();
                var transitionEndStates = stateListPair.getRight();

                for (var prevState : prevStates) {
                    for (var transitionStartState : transitionStartStates) {
                        addTransition(transitionBlock, prevState.name(), transitionStartState.name(), transitionStartState.intent());
                    }
                }

                if (startStates.isEmpty()) {
                    startStates = transitionStartStates;
                }

                prevStates = transitionEndStates;
            } else {
                String currentStateIntent = "";
                String currentStateName = "";

                if (child instanceof BMLParser.FunctionCallContext functionCallContext) {
                    var pair = addAnonymousStateForFunctionCall(functionCallContext);
                    currentStateName = pair.getLeft().stream()
                            .filter(n -> n instanceof VariableDeclarationExpr)
                            .map(n -> ((VariableDeclarationExpr) n).getVariable(0).getNameAsString())
                            .findAny().get();
                    currentStateIntent = ((BMLState) ((BMLFunctionType) functionCallContext.type).getReturnType()).getIntent();

                    stateActions.put(currentStateName, pair.getRight().getBody().asBlockStmt());

                    transitionBlock.getStatements().addAll(pair.getLeft().getStatements());
                } else if (child instanceof TerminalNode node && node.getSymbol().getType() == BMLParser.Identifier) {
                    currentStateName = node.getText();
                    currentStateIntent = ((BMLState) ((VariableSymbol) currentScope.resolve(currentStateName)).getType()).getIntent();
                    currentStateName += "State";
                }

                for (var prevState : prevStates) {
                    addTransition(transitionBlock, prevState.name(), currentStateName, currentStateIntent);
                }

                prevStates = List.of(new DialogueState(currentStateName, currentStateIntent));

                if (startStates.isEmpty()) {
                    startStates = prevStates;
                }
            }
        }

        // Add jump to default state (without executing action)
        for (var prevState : prevStates) {
            var block = stateActions.get(prevState.name());
            if (block != null) {
                addJumpToDefaultStateIfNotPresent(block);
            }
        }

        initMethodBody.getStatements().addAll(transitionBlock.getStatements());

        return new ImmutablePair<>(startStates, prevStates);
    }

    private Pair<BlockStmt, LambdaExpr> addAnonymousStateForFunctionCall(BMLParser.FunctionCallContext functionCallContext) {
        var functionType = (BMLFunctionType) functionCallContext.type;
        var functionName = functionCallContext.functionName.getText();
        var stateType = StaticJavaParser.parseClassOrInterfaceType("State");
        var stmts = new BlockStmt();
        var actionLambdaExpr = (LambdaExpr) generateLambdaExprForAction((BMLState) functionType.getReturnType());

        switch (functionName) {
            case "default" -> {
                var initializerExpr = new ObjectCreationExpr(null, stateType, new NodeList<>(actionLambdaExpr));
                var defaultStateField = dialogueClass.getFieldByName("defaultState").get();
                defaultStateField.getVariable(0).setInitializer(initializerExpr);
                defaultStateField.addModifier(Modifier.Keyword.FINAL);
            }
            case "initial", "state" -> {
                // Create variable
                var stateName = "state" + stateCounter++;
                addVariableForState(stmts, stateName, stateType, new NodeList<>(actionLambdaExpr), functionCallContext.getText());

                if (functionName.equals("initial")) {
                    addTransition(stmts, "defaultState", stateName, ((BMLState) functionType.getReturnType()).getIntent());
                }
            }
            default -> {}
        }

        return new ImmutablePair<>(stmts, actionLambdaExpr);
    }

    public Pair<List<DialogueState>, List<DialogueState>> visitDialogueTransitionList(BMLParser.DialogueTransitionListContext ctx, Scope currentScope) {
        List<DialogueState> startStates = new ArrayList<>();
        List<DialogueState> endStates = new ArrayList<>();
        var transitionBlock = new BlockStmt();

        for (var child : ctx.dialogueTransitionListItem()) {
            if (child.dialogueTransition() != null) {
                var stateListPair = visitDialogueTransition(child.dialogueTransition(), currentScope);
                startStates.addAll(stateListPair.getLeft());
                endStates.addAll(stateListPair.getRight());
            } else if (child.functionCall() != null) {
                var pair = addAnonymousStateForFunctionCall(child.functionCall());
                var stateName = pair.getLeft().stream()
                        .filter(n -> n instanceof VariableDeclarationExpr)
                        .map(n -> ((VariableDeclarationExpr) n).getVariable(0).getNameAsString())
                        .findAny().get();

                transitionBlock.getStatements().addAll(pair.getLeft().getStatements());

                var intent = ((BMLState) ((BMLFunctionType) child.functionCall().type).getReturnType()).getIntent();
                var currentState = new DialogueState(stateName, intent);
                startStates.add(currentState);
                endStates.add(currentState);

                stateActions.put(stateName, pair.getRight().getBody().asBlockStmt());
            } else { // Identifier
                var intent = ((BMLState) ((VariableSymbol) currentScope.resolve(child.getText())).getType()).getIntent();
                var currentState = new DialogueState(child.getText() + "State", intent);
                startStates.add(currentState);
                endStates.add(currentState);
            }
        }

        initMethodBody.getStatements().addAll(transitionBlock.getStatements());

        return new ImmutablePair<>(startStates, endStates);
    }
}
