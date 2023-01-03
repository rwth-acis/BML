package i5.bml.transpiler;

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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.dialogue.BMLState;
import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;
import i5.bml.transpiler.bot.dialogue.State;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DialogueAutomatonSynthesizer {

    private int stateCounter = 1;

    private final Set<String> stateNames = new HashSet<>();

    private final JavaSynthesizer javaSynthesizer;

    public DialogueAutomatonSynthesizer(JavaSynthesizer javaSynthesizer) {
        this.javaSynthesizer = javaSynthesizer;
    }

    public void visitDialogueBody(BMLParser.DialogueBodyContext ctx) {
        var dialogueHeadContext = ((BMLParser.DialogueAutomatonContext) ctx.parent).head;
        var newDialogueClassName = "%sDialogueAutomaton".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));
        var newActionsClassName = "%sActions".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));

        // Add constructor that invokes init() method
        Utils.readAndWriteClass(javaSynthesizer.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
            var constructor = clazz.addConstructor(Modifier.Keyword.PUBLIC);
            constructor.setBody(new BlockStmt().addStatement(new MethodCallExpr("initTransitions")));
        });

        // TODO: Do this in the procedural fashion by going with for each on `ctx.children`

        // Action definitions
        if (!ctx.dialogueFunctionDefinition().isEmpty()) {
            Utils.readAndWriteClass(javaSynthesizer.botOutputPath() + "dialogue", newActionsClassName, clazz -> {
                javaSynthesizer.classStack().push(clazz);
                for (var c : ctx.dialogueFunctionDefinition()) {
                    var actionMethod = clazz.addMethod(c.functionDefinition().head.functionName.getText(),
                            Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                    actionMethod.addParameter(MessageEventContext.class.getSimpleName(), "ctx");
                    //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
                    var compilationUnit = clazz.findCompilationUnit().get();

                    // Add import for `MessageEventContext`
                    compilationUnit.addImport(Utils.renameImport(MessageEventContext.class, javaSynthesizer.outputPackage()), false, false);

                    // We visit the whole function definition to have a scope created
                    actionMethod.setBody((BlockStmt) javaSynthesizer.visit(c.functionDefinition()));
                }
                javaSynthesizer.classStack().pop();
            });
        }

        // Assignments (States and other types)
        if (!ctx.dialogueAssignment().isEmpty()) {
            Utils.readAndWriteClass(javaSynthesizer.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
                javaSynthesizer.classStack().push(clazz);
                ctx.dialogueAssignment().forEach(c -> {
                    if (c.assignment().expr.type instanceof BMLState) {
                        // We have to visit the node to create a class for it
                        visitDialogueAssignment(c);
                    } else { // We have some "normal" type, add field with getter to class
                        var field = clazz.addFieldWithInitializer(BMLTypeResolver.resolveBMLTypeToJavaType(c.assignment().expr.type),
                                c.assignment().name.getText(), (Expression) javaSynthesizer.visit(c.assignment().expr),
                                Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                        var getter = field.createGetter();
                        // Remove "get" prefix
                        getter.setName(StringUtils.uncapitalize(getter.getNameAsString().substring(3)));
                    }
                });
                javaSynthesizer.classStack().pop();
            });
        }

        // State creation, i.e., state function calls
        if (!ctx.dialogueStateCreation().isEmpty()) {
            Utils.readAndWriteClass(javaSynthesizer.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
                javaSynthesizer.classStack().push(clazz);
                //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                var initMethodBody = clazz.getMethodsByName("initTransitions").get(0).getBody().get();
                for (var dialogueStateCreationContext : ctx.dialogueStateCreation()) {
                    var block = (BlockStmt) visitDialogueStateCreation(dialogueStateCreationContext, clazz);
                    initMethodBody.getStatements().addAll(block.getStatements());
                }
                javaSynthesizer.classStack().pop();
            });
        }

        // Take care of dialogue transitions
        for (var transition : ctx.dialogueTransition()) {
            Utils.readAndWriteClass(javaSynthesizer.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
                javaSynthesizer.classStack().push(clazz);
                javaSynthesizer.visit(transition);
                javaSynthesizer.classStack().pop();
            });
        }

        // We have to do sinks last, since we need to collect all states before
        Utils.readAndWriteClass(javaSynthesizer.botOutputPath() + "dialogue", newDialogueClassName, clazz -> {
            ctx.dialogueStateCreation().stream()
                    .filter(d -> d.functionCall().functionName.getText().equals("sink"))
                    .forEach(d -> {
                        var stateType = (BMLState) ((BMLFunctionType) d.functionCall().type).getReturnType();
                        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
                        var initMethodBody = clazz.getMethodsByName("initTransitions").get(0).getBody().get();

                        // Generate action for sink state
                        var actionLambdaExpr = generateLambdaExprForAction(stateType, clazz);

                        // The sink state jumps back to the default state after executing its action
                        addJumpToDefaultState(actionLambdaExpr.getBody().asBlockStmt());

                        // Create variable for sink state
                        var sinkName = "state" + stateCounter++;
                        addVariableForState(initMethodBody, sinkName, StaticJavaParser.parseClassOrInterfaceType("State"),
                                new NodeList<>(actionLambdaExpr), d.functionCall().getText());

                        // Add all transitions going into sink state
                        for (String stateName : stateNames) {
                            addTransition(initMethodBody, stateName, sinkName, stateType.getIntent());
                        }

                    });
        });
    }

    private void visitDialogueAssignment(BMLParser.DialogueAssignmentContext ctx) {
        var stateType = (BMLState) ctx.assignment().expr.type;

        // Create a class
        CompilationUnit c = new CompilationUnit();

        // Add package declaration
        c.setPackageDeclaration(javaSynthesizer.outputPackage() + "dialogue.states");

        // Set class name and extends
        var className = StringUtils.capitalize(ctx.assignment().name.getText()) + "State";
        var clazz = c.addClass(className);
        clazz.addExtendedType(State.class.getSimpleName());

        // Make import for extends
        c.addImport(Utils.renameImport(State.class, javaSynthesizer.outputPackage()), false, false);

        // Add field to store & set intent
        clazz.addField(DialogueAutomaton.class.getSimpleName(), "dialogueAutomaton", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        // Add import for `DialogueAutomaton`
        c.addImport(Utils.renameImport(DialogueAutomaton.class, javaSynthesizer.outputPackage()), false, false);

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
        actionMethod.setBody(generateBlockForStateAction(stateType, c, clazz));

        // TODO: When should this jump back to the default state?

        // Add import for action parameter of type `MessageEventContext`
        c.addImport(Utils.renameImport(MessageEventContext.class, javaSynthesizer.outputPackage()), false, false);

        // Create file at desired destination
        Utils.writeClass(javaSynthesizer.botOutputPath() + "dialogue/states", className, c);

        // Add state to init method
        var dialogueClass = javaSynthesizer.currentClass();
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var initMethodBody = dialogueClass.getMethodsByName("initTransitions").get(0).getBody().get();

        // Create variable
        var stateVarType = StaticJavaParser.parseClassOrInterfaceType(className);
        var stateName = ctx.assignment().name.getText() + "State";
        addVariableForState(initMethodBody, stateName, stateVarType, new NodeList<>(new NameExpr("this")), ctx.assignment().expr.getText());

        // Add state to global collection
        stateNames.add(stateName);

        // Add import for `className`
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        dialogueClass.findCompilationUnit().get().addImport(javaSynthesizer.outputPackage() + "dialogue.states." + className);

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

    private Node visitDialogueStateCreation(BMLParser.DialogueStateCreationContext ctx, ClassOrInterfaceDeclaration clazz) {
        var functionType = (BMLFunctionType) ctx.functionCall().type;
        var functionName = ctx.functionCall().functionName.getText();
        var stateType = StaticJavaParser.parseClassOrInterfaceType("State");

        return switch (functionName) {
            case "default" -> {
                var actionLambdaExpr = generateLambdaExprForAction((BMLState) functionType.getReturnType(), clazz);
                var initializerExpr = new ObjectCreationExpr(null, stateType, new NodeList<>(actionLambdaExpr));
                clazz.addFieldWithInitializer("State", "defaultState", initializerExpr, Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                yield new BlockStmt();
            }
            case "initial" -> {
                var stmts = new BlockStmt();

                var actionLambdaExpr = generateLambdaExprForAction((BMLState) functionType.getReturnType(), clazz);

                // Since the state has no transitions, we jump back to the default state to await new commands
                addJumpToDefaultState(actionLambdaExpr.getBody().asBlockStmt());

                // Create variable
                var stateName = "state" + stateCounter++;
                addVariableForState(stmts, stateName, stateType, new NodeList<>(actionLambdaExpr), ctx.functionCall().getText());

                // Add state to global collection
                stateNames.add(stateName);

                yield stmts;
            }
            default -> new BlockStmt();
        };
    }

    private void addTransition(BlockStmt block, String from, String to, String withIntent) {
        var intents = withIntent.split(",");
        for (String intent : intents) {
            var transition = new MethodCallExpr(new NameExpr(from), "addTransition",
                    new NodeList<>(new StringLiteralExpr(intent.replace(" ", "")), new NameExpr(to)));
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

    private void addVariableForState(BlockStmt block, String stateName, ClassOrInterfaceType stateType, NodeList<Expression> args,
                                     String commentText) {
        var stateObject = new ObjectCreationExpr(null, stateType, args);
        var stateVar = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), stateName, stateObject));
        // Add comment to indicate origin
        stateVar.setComment(new LineComment("State represents: %s".formatted(commentText)));

        block.addStatement(stateVar);
    }

    private void addJumpToDefaultState(BlockStmt block) {
        block.addStatement(new MethodCallExpr("jumpTo", new NameExpr("defaultState"), new NameExpr("ctx")));
    }

    private LambdaExpr generateLambdaExprForAction(BMLState stateType, ClassOrInterfaceDeclaration clazz) {
        // Create a lambda expression for specified action
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var lambdaBlock = generateBlockForStateAction(stateType, clazz.findCompilationUnit().get(), clazz);
        return new LambdaExpr(new Parameter(new UnknownType(), "ctx"), lambdaBlock);
    }

    private BlockStmt generateBlockForStateAction(BMLState stateType, CompilationUnit c, ClassOrInterfaceDeclaration clazz) {
        return switch (stateType.getActionType().getClass().getAnnotation(BMLType.class).name()) {
            case STRING -> {
                c.addImport(Utils.renameImport(MessageHelper.class, javaSynthesizer.outputPackage()), false, false);
                yield new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                        new NodeList<>(new NameExpr("ctx"), (StringLiteralExpr) javaSynthesizer.visit(stateType.getAction()))));
            }
            case LIST -> {
                // Add import for `Random`
                c.addImport(Utils.renameImport(Random.class, javaSynthesizer.outputPackage()), false, false);

                var randomType = StaticJavaParser.parseClassOrInterfaceType("Random");
                clazz.addFieldWithInitializer(Random.class, "random",
                        new ObjectCreationExpr(null, randomType, new NodeList<>()), Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                var listNameExpr = (Expression) javaSynthesizer.visit(stateType.getAction());
                var getRandomListEntry = new MethodCallExpr(listNameExpr, "get", new NodeList<>(new NameExpr("nextMessage")));

                var listSizeExpr = new MethodCallExpr(listNameExpr, "size");
                var getRandomIntStmt = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), "nextMessage",
                        new MethodCallExpr(new NameExpr("random"), "nextInt", new NodeList<>(listSizeExpr))));

                var sendMessageStmt = new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                        new NodeList<>(new NameExpr("ctx"), getRandomListEntry));

                yield new BlockStmt().addStatement(getRandomIntStmt).addStatement(sendMessageStmt);
            }
            case FUNCTION -> {
                var currentClass = javaSynthesizer.currentClass();
                var actionsClassName = currentClass.getNameAsString().replace("DialogueAutomaton", "") + "Actions";

                // Add import for `<name>Actions`
                //noinspection OptionalGetWithoutIsPresent -> We can assume the classes presence
                var packageName = currentClass.findCompilationUnit().get().getPackageDeclaration().get().getNameAsString();
                c.addImport(packageName + actionsClassName, false, false);

                var identifier = (Token) javaSynthesizer.visit(stateType.getAction());
                var actionCall = new MethodCallExpr(new NameExpr(actionsClassName), identifier.getText(), new NodeList<>(new NameExpr("ctx")));
                yield new BlockStmt().addStatement(actionCall);
            }
            default -> new BlockStmt();
        };
    }

    public Node visitDialogueTransition(BMLParser.DialogueTransitionContext ctx, Scope currentScope) {
        // TODO
        return null;
    }
}
