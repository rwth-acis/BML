package i5.bml.transpiler.generators.dialogue;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.dialogue.BMLState;
import i5.bml.transpiler.bot.config.BotConfig;
import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;
import i5.bml.transpiler.bot.dialogue.State;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.generators.types.BMLTypeResolver;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class DialogueAutomatonSynthesizer {

    private int stateCounter = 1;

    private final JavaTreeGenerator javaTreeGenerator;

    public DialogueAutomatonSynthesizer(JavaTreeGenerator javaTreeGenerator) {
        this.javaTreeGenerator = javaTreeGenerator;
    }

    public void visitDialogueBody(BMLParser.DialogueBodyContext ctx, Scope currentScope) {
        var dialogueHeadContext = ((BMLParser.DialogueAutomatonContext) ctx.parent).head;
        var newDialogueClassName = "%sDialogueAutomaton".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));
        var newActionsClassName = "%sActions".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));

        // Add constructor that invokes init() method
        PrinterUtil.readAndWriteClass(javaTreeGenerator.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
            var constructor = clazz.addConstructor(Modifier.Keyword.PUBLIC);
            constructor.setBody(new BlockStmt().addStatement(new MethodCallExpr("initTransitions")));
        });

        // TODO: Do this in the procedural fashion by going with for each on `ctx.children`

        // Action definitions
        if (!ctx.dialogueFunctionDefinition().isEmpty()) {
            PrinterUtil.readAndWriteClass(javaTreeGenerator.botOutputPath() + "dialogue", newActionsClassName, clazz -> {
                javaTreeGenerator.classStack().push(clazz);
                for (var c : ctx.dialogueFunctionDefinition()) {
                    var actionMethod = clazz.addMethod(c.functionDefinition().head.functionName.getText(),
                            Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                    actionMethod.addParameter(MessageEventContext.class.getSimpleName(), "ctx");
                    //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
                    var compilationUnit = clazz.findCompilationUnit().get();

                    // Add import for `MessageEventContext`
                    compilationUnit.addImport(Utils.renameImport(MessageEventContext.class, javaTreeGenerator.outputPackage()), false, false);

                    // We visit the whole function definition to have a scope created
                    actionMethod.setBody((BlockStmt) javaTreeGenerator.visit(c.functionDefinition()));
                }
                javaTreeGenerator.classStack().pop();
            });
        }

        // Set currentState = defaultState as first statement in `initTransitions` from dialogue class
        PrinterUtil.readAndWriteClass(javaTreeGenerator.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
            //noinspection OptionalGetWithoutIsPresent -> We can assume presence
            var initMethodBody = clazz.getMethodsByName("initTransitions").get(0).getBody().get();
            initMethodBody.addStatement(new AssignExpr(new NameExpr("currentState"), new NameExpr("defaultState"), AssignExpr.Operator.ASSIGN));
        });

        // Assignments (States and other types)
        if (!ctx.dialogueAssignment().isEmpty()) {
            PrinterUtil.readAndWriteClass(javaTreeGenerator.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
                javaTreeGenerator.classStack().push(clazz);
                ctx.dialogueAssignment().forEach(c -> {
                    if (c.assignment().expr.type instanceof BMLState) {
                        // We have to visit the node to create a class for it
                        visitDialogueAssignment(c);
                    } else { // We have some "normal" type, add field with getter to class
                        var field = clazz.addFieldWithInitializer(BMLTypeResolver.resolveBMLTypeToJavaType(c.assignment().expr.type),
                                c.assignment().name.getText(), (Expression) javaTreeGenerator.visit(c.assignment().expr),
                                Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                        var getter = field.createGetter();
                        // Remove "get" prefix
                        getter.setName(StringUtils.uncapitalize(getter.getNameAsString().substring(3)));
                    }
                });
                javaTreeGenerator.classStack().pop();
            });
        }

        // State creation, i.e., state function calls
        if (!ctx.dialogueStateCreation().isEmpty()) {
            PrinterUtil.readAndWriteClass(javaTreeGenerator.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
                javaTreeGenerator.classStack().push(clazz);
                //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                var initMethodBody = clazz.getMethodsByName("initTransitions").get(0).getBody().get();

                for (var dialogueStateCreationContext : ctx.dialogueStateCreation()) {
                    var block = visitDialogueStateCreation(dialogueStateCreationContext, clazz);
                    initMethodBody.getStatements().addAll(block.getStatements());
                }
                javaTreeGenerator.classStack().pop();
            });
        }

        // Take care of dialogue transitions
        for (var transition : ctx.dialogueTransition()) {
            PrinterUtil.readAndWriteClass(javaTreeGenerator.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
                javaTreeGenerator.classStack().push(clazz);
                //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                var initMethodBody = clazz.getMethodsByName("initTransitions").get(0).getBody().get();
                var stateListPair = visitDialogueTransition(transition, currentScope);
                for (var state : stateListPair.getRight()) {
                    addTransitionToDefaultState(initMethodBody, state.name());
                }
                javaTreeGenerator.classStack().pop();
            });
        }

        // We have to do sinks last, since we need to collect all states before
        PrinterUtil.readAndWriteClass(javaTreeGenerator.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
            ctx.dialogueStateCreation().stream()
                    .filter(d -> d.functionCall().functionName.getText().equals("sink"))
                    .forEach(d -> {
                        var stateType = (BMLState) ((BMLFunctionType) d.functionCall().type).getReturnType();
                        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                        var initMethodBody = clazz.getMethodsByName("initTransitions").get(0).getBody().get();

                        // Generate action for sink state
                        var actionLambdaExpr = generateLambdaExprForAction(stateType, clazz);

                        // The sink state jumps back to the default state after executing its action
                        addJumpToDefaultState(((LambdaExpr) actionLambdaExpr).getBody().asBlockStmt());

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
        });
    }

    private void visitDialogueAssignment(BMLParser.DialogueAssignmentContext ctx) {
        var stateType = (BMLState) ctx.assignment().expr.type;

        // Create a class
        CompilationUnit c = new CompilationUnit();

        // Add package declaration
        c.setPackageDeclaration(javaTreeGenerator.outputPackage() + "dialogue.states");

        // Set class name and extends
        var className = StringUtils.capitalize(ctx.assignment().name.getText()) + "State";
        var clazz = c.addClass(className);
        clazz.addExtendedType(State.class.getSimpleName());

        // Make import for extends
        c.addImport(Utils.renameImport(State.class, javaTreeGenerator.outputPackage()), false, false);

        // Add field to store & set intent
        clazz.addField(DialogueAutomaton.class.getSimpleName(), "dialogueAutomaton", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        // Add import for `DialogueAutomaton`
        c.addImport(Utils.renameImport(DialogueAutomaton.class, javaTreeGenerator.outputPackage()), false, false);

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
        actionMethod.setBody((BlockStmt) generateBlockForStateAction(stateType, c, clazz, false));

        // TODO: When should this jump back to the default state?

        // Add import for action parameter of type `MessageEventContext`
        c.addImport(Utils.renameImport(MessageEventContext.class, javaTreeGenerator.outputPackage()), false, false);

        // Create file at desired destination
        PrinterUtil.writeClass(javaTreeGenerator.botOutputPath() + "dialogue/states", className, c);

        // Add state to init method
        var dialogueClass = javaTreeGenerator.currentClass();
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var initMethodBody = dialogueClass.getMethodsByName("initTransitions").get(0).getBody().get();

        // Create variable
        var stateVarType = StaticJavaParser.parseClassOrInterfaceType(className);
        var stateName = ctx.assignment().name.getText() + "State";
        addVariableForState(initMethodBody, stateName, stateVarType, new NodeList<>(new NameExpr("this")), ctx.assignment().expr.getText());

        // Add import for `className`
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        dialogueClass.findCompilationUnit().get().addImport(javaTreeGenerator.outputPackage() + "dialogue.states." + className);

        // Add transitions depending on function type
        var funcCall = ctx.assignment().expr.functionCall();
        if (funcCall != null) {
            switch (funcCall.functionName.getText()) {
                case "initial" ->
                        addTransitionsForInitialState(initMethodBody, stateName, ((BMLFunctionType) funcCall.type));
                case "state" -> addTransitionToDefaultState(initMethodBody, stateName);
                default -> {
                }
            }
        }
    }

    private BlockStmt visitDialogueStateCreation(BMLParser.DialogueStateCreationContext ctx, ClassOrInterfaceDeclaration clazz) {
        return addAnonymousStateForFunctionCall(ctx.functionCall(), clazz);
    }

    private void addTransition(BlockStmt block, String from, String to, String withIntent) {
        var intents = withIntent.split(",");
        for (String intent : intents) {
            intent = intent.replaceAll(" ", "");
            Expression intentExpr;
            if (intent.equals("_")) {
                intentExpr = new FieldAccessExpr(new NameExpr(BotConfig.class.getSimpleName()), "NLU_FALLBACK_INTENT");
                //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                var compilationUnit = javaTreeGenerator.currentClass().findCompilationUnit().get();
                compilationUnit.addImport(Utils.renameImport(BotConfig.class, javaTreeGenerator.outputPackage()), false, false);
            } else {
                intentExpr = new StringLiteralExpr(intent);
            }
            var transition = new MethodCallExpr(new NameExpr(from), "addTransition",
                    new NodeList<>(intentExpr, new NameExpr(to)));
            block.addStatement(transition);
        }
    }

    private void addTransitionsForInitialState(BlockStmt block, String stateName, BMLFunctionType functionType) {
        // Add transition(s) default -> state since it is initial (for every intent)
        addTransition(block, "defaultState", stateName, ((BMLState) functionType.getReturnType()).getIntent());

        addTransitionToDefaultState(block, stateName);
    }

    private void addTransitionToDefaultState(BlockStmt block, String stateName) {
        // Add a transition from state -> default (every state has this)
        addTransition(block, stateName, "defaultState", "_");
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

    private void addJumpToDefaultState(BlockStmt block) {
        block.addStatement(new MethodCallExpr("jumpTo", new NameExpr("defaultState"), new NameExpr("ctx")));
    }

    private Expression generateLambdaExprForAction(BMLState stateType, ClassOrInterfaceDeclaration clazz) {
        // Create a lambda expression for specified action
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var node = generateBlockForStateAction(stateType, clazz.findCompilationUnit().get(), clazz, true);
        if (node instanceof BlockStmt blockStmt) {
            return new LambdaExpr(new Parameter(new UnknownType(), "ctx"), blockStmt);
        } else if (node instanceof MethodReferenceExpr methodReference) {
            return methodReference;
        } else { // MethodCallExpr
            return new LambdaExpr(new Parameter(new UnknownType(), "ctx"), (MethodCallExpr) node);
        }
    }

    private Node generateBlockForStateAction(BMLState stateType, CompilationUnit c, ClassOrInterfaceDeclaration clazz,
                                             boolean isLambda) {
        return switch (stateType.getActionType().getClass().getAnnotation(BMLType.class).name()) {
            case STRING -> {
                c.addImport(Utils.renameImport(MessageHelper.class, javaTreeGenerator.outputPackage()), false, false);
                yield new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                        new NodeList<>(new NameExpr("ctx"), (StringLiteralExpr) javaTreeGenerator.visit(stateType.getAction()))));
            }
            case LIST -> {
                // Add import for `Random`
                c.addImport(Utils.renameImport(Random.class, javaTreeGenerator.outputPackage()), false, false);

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
                var currentClass = javaTreeGenerator.currentClass();
                var actionsClassName = currentClass.getNameAsString().replace("DialogueAutomaton", "") + "Actions";

                // Add import for `<name>Actions`
                //noinspection OptionalGetWithoutIsPresent -> We can assume the classes presence
                var packageName = currentClass.findCompilationUnit().get().getPackageDeclaration().get().getNameAsString();
                c.addImport(packageName + actionsClassName, false, false);

                var identifier = (NameExpr) javaTreeGenerator.visit(stateType.getAction());

                if (isLambda) {
                    yield new MethodReferenceExpr(new NameExpr(actionsClassName), new NodeList<>(), identifier.getNameAsString());
                } else {
                    yield new BlockStmt().addStatement(new MethodCallExpr(new NameExpr(actionsClassName), identifier.getName(), new NodeList<>(new NameExpr("ctx"))));
                }
            }
            case STATE -> {
                var functionType = (BMLFunctionType) stateType.getAction().functionCall().type;
                var destinationState = (NameExpr) javaTreeGenerator.visit(functionType.getRequiredParameters().get(0).getExprCtx());
                destinationState.setName(destinationState.getNameAsString() + "State");
                var methodCallExpr = new MethodCallExpr(null, "jumpTo", new NodeList<>(destinationState, new NameExpr("ctx")));

                if (isLambda) {
                    yield methodCallExpr;
                } else {
                    yield new BlockStmt().addStatement(methodCallExpr);
                }
            }
            default -> new BlockStmt();
        };
    }

    public Pair<List<DialogueState>, List<DialogueState>> visitDialogueTransition(BMLParser.DialogueTransitionContext ctx, Scope currentScope) {
        List<DialogueState> prevStates = new ArrayList<>();
        List<DialogueState> startStates = new ArrayList<>();
        var currentClass = javaTreeGenerator.currentClass();
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
                    var block = addAnonymousStateForFunctionCall(functionCallContext, currentClass);
                    //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                    currentStateName = block.stream()
                            .filter(n -> n instanceof VariableDeclarationExpr)
                            .map(n -> ((VariableDeclarationExpr) n).getVariable(0).getNameAsString())
                            .findAny().get();
                    currentStateIntent = ((BMLState) ((BMLFunctionType) functionCallContext.type).getReturnType()).getIntent();

                    transitionBlock.getStatements().addAll(block.getStatements());
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

        // TODO:
        // Add jumps to default state at the end of actions of last states in dialogue transition
        // But only if there is no jumpTo already & the last state is not an anonymous state
        for (var prevState : prevStates) {

        }

        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var initMethodBody = currentClass.getMethodsByName("initTransitions").get(0).getBody().get();
        initMethodBody.getStatements().addAll(transitionBlock.getStatements());

        return new ImmutablePair<>(startStates, prevStates);
    }

    private BlockStmt addAnonymousStateForFunctionCall(BMLParser.FunctionCallContext functionCallContext, ClassOrInterfaceDeclaration clazz) {
        var functionType = (BMLFunctionType) functionCallContext.type;
        var functionName = functionCallContext.functionName.getText();
        var stateType = StaticJavaParser.parseClassOrInterfaceType("State");
        var stmts = new BlockStmt();
        var actionLambdaExpr = generateLambdaExprForAction((BMLState) functionType.getReturnType(), clazz);

        return switch (functionName) {
            case "default" -> {
                var initializerExpr = new ObjectCreationExpr(null, stateType, new NodeList<>(actionLambdaExpr));
                clazz.addFieldWithInitializer("State", "defaultState", initializerExpr, Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                yield stmts;
            }
            case "initial", "state" -> {
                // TODO: Determine this
                // Since the state has no transitions, we jump back to the default state to await new commands
                //addJumpToDefaultState(actionLambdaExpr.getBody().asBlockStmt());

                // Create variable
                var stateName = "state" + stateCounter++;
                addVariableForState(stmts, stateName, stateType, new NodeList<>(actionLambdaExpr), functionCallContext.getText());

                if (functionName.equals("initial")) {
                    addTransitionsForInitialState(stmts, stateName, functionType);
                } else {
                    addTransitionToDefaultState(stmts, stateName);
                }

                yield stmts;
            }
            default -> stmts;
        };
    }

    public Pair<List<DialogueState>, List<DialogueState>> visitDialogueTransitionList(BMLParser.DialogueTransitionListContext ctx, Scope currentScope) {
        List<DialogueState> startStates = new ArrayList<>();
        List<DialogueState> endStates = new ArrayList<>();
        var currentClass = javaTreeGenerator.currentClass();
        var transitionBlock = new BlockStmt();

        for (var child : ctx.dialogueTransitionListItem()) {
            if (child.dialogueTransition() != null) {
                var stateListPair = visitDialogueTransition(child.dialogueTransition(), currentScope);
                startStates.addAll(stateListPair.getLeft());
                endStates.addAll(stateListPair.getRight());
            } else if (child.functionCall() != null) {
                var block = addAnonymousStateForFunctionCall(child.functionCall(), currentClass);
                //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                var stateName = block.stream()
                        .filter(n -> n instanceof VariableDeclarationExpr)
                        .map(n -> ((VariableDeclarationExpr) n).getVariable(0).getNameAsString())
                        .findAny().get();

                transitionBlock.getStatements().addAll(block.getStatements());

                var intent = ((BMLState) ((BMLFunctionType) child.functionCall().type).getReturnType()).getIntent();
                var currentState = new DialogueState(stateName, intent);
                startStates.add(currentState);
                endStates.add(currentState);
            } else { // Identifier
                var intent = ((BMLState) ((VariableSymbol) currentScope.resolve(child.getText())).getType()).getIntent();
                var currentState = new DialogueState(child.getText() + "State", intent);
                startStates.add(currentState);
                endStates.add(currentState);
            }
        }

        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var initMethodBody = currentClass.getMethodsByName("initTransitions").get(0).getBody().get();
        initMethodBody.getStatements().addAll(transitionBlock.getStatements());

        return new ImmutablePair<>(startStates, endStates);
    }
}
