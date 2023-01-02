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

import java.util.Random;

public class DialogueAutomatonSynthesizer {

    private final JavaSynthesizer javaSynthesizer;

    private int stateCounter = 1;

    public DialogueAutomatonSynthesizer(JavaSynthesizer javaSynthesizer) {
        this.javaSynthesizer = javaSynthesizer;
    }

    public void visitDialogueBody(BMLParser.DialogueBodyContext ctx) {
        var dialogueHeadContext = ((BMLParser.DialogueAutomatonContext) ctx.parent).head;
        var newDialogueClassName = "%sDialogueAutomaton".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));
        var newActionsClassName = "%sActions".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));

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
                        javaSynthesizer.visit(c);
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
                var initMethodBody = clazz.getMethodsByName("init").get(0).getBody().get();
                for (var dialogueStateCreationContext : ctx.dialogueStateCreation()) {
                    var block = (BlockStmt) visitDialogueStateCreation(dialogueStateCreationContext, clazz);

                    if (!block.isEmpty()) {
                        var comment = "State represents: %s".formatted(dialogueStateCreationContext.functionCall().getText());
                        block.getStatement(0).setComment(new LineComment(comment));
                    }

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

        // We have to do sinks last, since we need
    }

    public Node visitDialogueAssignment(BMLParser.DialogueAssignmentContext ctx) {
        var childNode = javaSynthesizer.visitChildren(ctx);

        if (ctx.assignment().expr.type instanceof BMLState stateType) {
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

            // Add import for action parameter of type `MessageEventContext`
            c.addImport(Utils.renameImport(MessageEventContext.class, javaSynthesizer.outputPackage()), false, false);

            // Create file at desired destination
            Utils.writeClass(javaSynthesizer.botOutputPath() + "dialogue/states", className, c);
        }

        return childNode;
    }

    public Node visitDialogueStateCreation(BMLParser.DialogueStateCreationContext ctx, ClassOrInterfaceDeclaration clazz) {
        var functionType = (BMLFunctionType) ctx.functionCall().type;
        var functionName = ctx.functionCall().functionName.getText();
        var stateType = StaticJavaParser.parseClassOrInterfaceType("State");

        return switch (functionName) {
            case "default" -> {
                var actionLambdaExpr = generateLambdaExprForAction((BMLState) functionType.getReturnType(), clazz);
                var initializerExpr = new ObjectCreationExpr(null, stateType, new NodeList<>(actionLambdaExpr));
                clazz.addFieldWithInitializer("State", "defaultState", initializerExpr, Modifier.Keyword.FINAL);

                yield new BlockStmt();
            }
            case "initial" -> {
                var stmts = new BlockStmt();

                var actionLambdaExpr = generateLambdaExprForAction((BMLState) functionType.getReturnType(), clazz);

                // Create variable
                var stateObject = new ObjectCreationExpr(null, stateType, new NodeList<>(actionLambdaExpr));
                var stateName = "state" + stateCounter;
                ++stateCounter;
                var stateVar = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), stateName, stateObject));
                stmts.addStatement(stateVar);

                // Add a transition from default -> state since it is initial
                var intentParameter = new StringLiteralExpr(((BMLState) functionType.getReturnType()).getIntent());
                stmts.addStatement(new MethodCallExpr(new NameExpr("defaultState"), "addTransition",
                        new NodeList<>(intentParameter, new NameExpr(stateName))));

                // Add a transition from state -> default since it is a single state creation call
                var stateTransitionToDefault = new MethodCallExpr(new NameExpr(stateName), "addTransition",
                        new NodeList<>(new StringLiteralExpr("_"), new NameExpr("defaultState")));
                stmts.addStatement(stateTransitionToDefault);

                yield stmts;
            }
            default -> new BlockStmt();
        };
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

                var getRandomIntStmt = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), "nextMessage",
                        new MethodCallExpr(new NameExpr("random"), "nextInt", new NodeList<>(new IntegerLiteralExpr("3")))));

                var getRandomListEntry = new MethodCallExpr((Expression) javaSynthesizer.visit(stateType.getAction()), "get",
                        new NodeList<>(new NameExpr("nextMessage")));

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
        var children = ctx.children;
        var firstState = children.get(0);
        var stateCounter = 1;
        var stateClassType = StaticJavaParser.parseClassOrInterfaceType("State");
        String currStateName = "";
        var initBlock = new BlockStmt();
        String intent = "";

        if (firstState instanceof BMLParser.FunctionCallContext funcCall) {
            currStateName = "state" + stateCounter;
            var newStateExpr = new ObjectCreationExpr(null, stateClassType, new NodeList<>());
            initBlock.addStatement(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), currStateName, newStateExpr)));
            ++stateCounter;

            // Retrieve intent
            intent = ((BMLState) ((BMLFunctionType) funcCall.type).getReturnType()).getIntent();
        } else if (firstState instanceof TerminalNode node && node.getSymbol().getType() == BMLParser.Identifier) {
            currStateName = firstState.getText();
            var className = StaticJavaParser.parseClassOrInterfaceType(StringUtils.capitalize(currStateName) + "State");
            var newStateExpr = new ObjectCreationExpr(null, className, new NodeList<>());
            initBlock.addStatement(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), currStateName, newStateExpr)));
            initBlock.addStatement(new MethodCallExpr(new NameExpr("namedStates"), "put", new NodeList<>(
                    new StringLiteralExpr(currStateName),
                    new NameExpr(currStateName)
            )));

            // Retrieve intent
            var nodeType = ((VariableSymbol) currentScope.resolve(node.getText())).getType();
            intent = ((BMLState) nodeType).getIntent();
        }

        // Add transition from default state
        if (ctx.parent instanceof BMLParser.DialogueBodyContext) {
            new MethodCallExpr(new NameExpr("defaultState"), "addTransition",
                    new NodeList<>(new StringLiteralExpr(intent), new NameExpr(currStateName)));
        }

        for (int i = 1, childrenSize = children.size(); i < childrenSize; i++) {
            var child = children.get(i);
            if (child instanceof BMLParser.FunctionCallContext) {

            } else if (child instanceof TerminalNode node && node.getSymbol().getType() == BMLParser.Identifier) {

            } else if (child instanceof BMLParser.DialogueTransitionListContext) {
                // TODO: Unpack what is returned and add to target states of first state
                var states = javaSynthesizer.visit(child);
            }
        }

        // Add body to init method
//        Utils.readAndWriteClass("dialogue", "DialogueAutomaton", clazz -> {
//            clazz.getMethodsByName("init").get(0).setBody(initBlock);
//        });

        return javaSynthesizer.visitChildren(ctx);
    }
}
