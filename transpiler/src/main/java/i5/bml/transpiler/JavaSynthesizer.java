package i5.bml.transpiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.symbols.BlockScope;
import i5.bml.parser.types.*;
import i5.bml.parser.types.annotations.BMLRoutineAnnotation;
import i5.bml.parser.types.dialogue.BMLState;
import i5.bml.transpiler.bot.dialogue.DialogueAutomatonTemplate;
import i5.bml.transpiler.bot.dialogue.State;
import i5.bml.transpiler.bot.events.Context;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.GeneratorRegistry;
import i5.bml.transpiler.utils.Utils;
import jdk.jshell.execution.Util;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaSynthesizer extends BMLBaseVisitor<Node> {

    private final String botOutputPath;

    private final String outputPackage;

    private Scope currentScope;

    private Scope globalScope;

    private Scope dialogueScope = new BlockScope(null);

    private final Stack<ClassOrInterfaceDeclaration> classStack = new Stack<>();

    private boolean wrapAssignmentInTryStmt = false;

    public JavaSynthesizer(String botOutputPath, String outputPackage) {
        this.botOutputPath = botOutputPath;
        this.outputPackage = outputPackage;
    }

    private void pushScope(Scope s) {
        currentScope = s;
    }

    private void popScope() {
        currentScope = currentScope.getEnclosingScope();
    }

    public String getBotOutputPath() {
        return botOutputPath;
    }

    public String getOutputPackage() {
        return outputPackage;
    }

    public Stack<ClassOrInterfaceDeclaration> getClassStack() {
        return classStack;
    }

    public ClassOrInterfaceDeclaration getCurrentClass() {
        return classStack.peek();
    }

    public void setWrapAssignmentInTryStmt(boolean wrapAssignmentInTryStmt) {
        this.wrapAssignmentInTryStmt = wrapAssignmentInTryStmt;
    }

    @Override
    public Node visitBotDeclaration(BMLParser.BotDeclarationContext ctx) {
        pushScope(ctx.scope);
        globalScope = ctx.scope;
        var result = super.visitBotDeclaration(ctx);
        popScope();
        return result;
    }

    @Override
    public Node visitBotHead(BMLParser.BotHeadContext ctx) {
        Utils.readAndWriteClass(botOutputPath, "BotConfig", clazz -> {
            for (var pair : ctx.params.elementExpressionPair()) {
                var type = BMLTypeResolver.resolveBMLTypeToJavaType(pair.expr.type);
                var name = pair.name.getText().toUpperCase();
                clazz.addFieldWithInitializer(type, name, (Expression) visit(pair.expr),
                        Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
            }
        });

        return null;
    }

    @Override
    public Node visitBotBody(BMLParser.BotBodyContext ctx) {
        // Components
        Utils.readAndWriteClass(botOutputPath + "/components", "ComponentRegistry", clazz -> {
            classStack.push(clazz);
            ctx.component().forEach(this::visit);
            classStack.pop();
        });

        if (!ctx.dialogueAutomaton().isEmpty()) {
            // Dialogues
            for (var dialogueAutomatonContext : ctx.dialogueAutomaton()) {
                var newDialogueClassName = "%sDialogueAutomaton".formatted(StringUtils.capitalize(dialogueAutomatonContext.head.name.getText()));
                var newActionsClassName = "%sActions".formatted(StringUtils.capitalize(dialogueAutomatonContext.head.name.getText()));

                // Duplicate DialogueAutomaton.java and Actions.java
                try {
                    FileUtils.copyFile(new File(botOutputPath + "dialogue/DialogueAutomatonTemplate.java"),
                            new File("%s/dialogue/%s.java".formatted(botOutputPath, newDialogueClassName)));
                    Utils.readAndWriteClass("%s/dialogue".formatted(botOutputPath), newDialogueClassName, "DialogueAutomatonTemplate", clazz -> {
                        clazz.setName(newDialogueClassName);
                    });

                    FileUtils.copyFile(new File(botOutputPath + "dialogue/Actions.java"),
                            new File("%s/dialogue/%s.java".formatted(botOutputPath, newActionsClassName)));
                    Utils.readAndWriteClass("%s/dialogue".formatted(botOutputPath), newActionsClassName, "Actions", clazz -> {
                        clazz.setName(newActionsClassName);
                    });
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

                visit(dialogueAutomatonContext);
            }
            // After having copied everything, we delete the original
            try {
                FileUtils.forceDelete(new File(botOutputPath + "dialogue/DialogueAutomatonTemplate.java"));
                FileUtils.forceDelete(new File(botOutputPath + "dialogue/Actions.java"));

                if (ctx.dialogueAutomaton().isEmpty()) {
                    // When there is no dialogue, we can delete the whole folder
                    FileUtils.deleteDirectory(new File(botOutputPath + "dialogue"));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // We remove the DialogueAutomaton references when there is no dialogue specified
            Utils.readAndWriteClass(botOutputPath + "/threads", "Session", clazz -> {
                //noinspection OptionalGetWithoutIsPresent
                clazz.findCompilationUnit().get().getImports().clear();
                clazz.getFieldByName("dialogue").get().remove();
                clazz.getMethodsByName("getDialogue").get(0).remove();

            });
        }


        // Function definitions
        var routineCount = 0;
        for (var functionContext : ctx.functionDefinition()) {
            for (var annotationContext : functionContext.annotation()) {
                var generator = GeneratorRegistry.getGeneratorForType(annotationContext.type);
                generator.populateClassWithFunction(functionContext, annotationContext, this);

                if (annotationContext.type instanceof BMLRoutineAnnotation) {
                    ++routineCount;
                }
            }
        }

        // Set number of routines for scheduler pool size
        int finalRoutineCount = routineCount;
        Utils.readAndWriteClass(botOutputPath, "BotConfig", clazz -> {
            //noinspection OptionalGetWithoutIsPresent
            clazz.getFieldByName("ROUTINE_COUNT").get().getVariables().get(0).setInitializer(new IntegerLiteralExpr("" + finalRoutineCount));
        });

        // We have visited all children already, and we have nothing to return
        return null;
    }

    @Override
    public Node visitComponent(BMLParser.ComponentContext ctx) {
        GeneratorRegistry.getGeneratorForType(ctx.type).generateComponent(ctx, this);
        return null;
    }

    @Override
    public Node visitFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        if (ctx.parent instanceof BMLParser.DialogueFunctionDefinitionContext) {
            return super.visitFunctionDefinition(ctx);
        }

        pushScope(ctx.scope);
        var result = super.visitFunctionDefinition(ctx);
        popScope();
        return result;
    }

    @Override
    public Node visitStatement(BMLParser.StatementContext ctx) {
        if (ctx.ifStatement() != null || ctx.forEachStatement() != null || ctx.block() != null) {
            pushScope(ctx.scope);
            var result = super.visitStatement(ctx);
            popScope();
            return result;
        } else {
            return super.visitStatement(ctx);
        }
    }

    @Override
    public Node visitBlock(BMLParser.BlockContext ctx) {
        return new BlockStmt(ctx.statement().stream()
                .flatMap(statementContext -> {
                    var node = visit(statementContext);
                    return node instanceof BlockStmt block ? block.getStatements().stream() : Stream.of(node);
                })
                .map(node -> !(node instanceof Statement) ? new ExpressionStmt((Expression) node) : (Statement) node)
                .collect(Collectors.toCollection(NodeList::new))
        );
    }

    @Override
    public Node visitIfStatement(BMLParser.IfStatementContext ctx) {
        var elseStmt = ctx.elseStmt == null ? null : (Statement) visit(ctx.elseStmt);
        return new IfStmt((Expression) visit(ctx.expr), (Statement) visit(ctx.thenStmt), elseStmt);
    }

    /**
     * We cannot use .forEach(Consumer c) because the consumer c expects final variables,
     * this would greatly complicate code generation. Hence, we go for the simple good 'n' old
     * `enhanced for statement`:<br>
     * <pre>
     *     for (var i : list) { // For lists
     *         // Do something
     *     }
     *
     *     for (var e : map.entrySet()) {
     *         var key = e.getKey();
     *         var value = e.getValue();
     *         // Do something
     *     }
     * </pre>
     *
     * @param ctx the parse tree
     * @return the freshly created forEach statement instance of
     * <a href="https://javadoc.io/doc/com.github.javaparser/javaparser-core/latest/index.html">ForEachStmt</a>
     * @implNote The lists or maps we are working on are synchronized or concurrent by construction.
     */
    @Override
    public Node visitForEachStatement(BMLParser.ForEachStatementContext ctx) {
        VariableDeclarationExpr variable;
        BlockStmt forEachBody = (BlockStmt) visit(ctx.forEachBody());
        var iterable = (Expression) visit(ctx.expr);

        if (ctx.comma == null) { // List
            variable = new VariableDeclarationExpr(new VarType(), ctx.Identifier(0).getText());
        } else { // Map
            var mapEntryVarName = "entry";
            variable = new VariableDeclarationExpr(new VarType(), mapEntryVarName);

            forEachBody.addStatement(0, new VariableDeclarationExpr(new VariableDeclarator(new VarType(),
                    ctx.Identifier(0).getText(), new MethodCallExpr(new NameExpr(mapEntryVarName), "getKey"))));
            forEachBody.addStatement(0, new VariableDeclarationExpr(new VariableDeclarator(new VarType(),
                    ctx.Identifier(1).getText(), new MethodCallExpr(new NameExpr(mapEntryVarName), "getValue"))));

            iterable = new MethodCallExpr(iterable, "entrySet");
        }

        return new ForEachStmt(variable, iterable, forEachBody);
    }

    @Override
    public Node visitAssignment(BMLParser.AssignmentContext ctx) {
        if (ctx.op.getType() == BMLParser.ASSIGN) {
            var node = visit(ctx.expr);
            if (wrapAssignmentInTryStmt) {
                // Reset
                wrapAssignmentInTryStmt = false;

                var block = new BlockStmt();
                var tryBlock = new BlockStmt();
                var varName = ctx.name.getText();

                if (ctx.expr.type.getName().equals("empty")) {
                    tryBlock.addStatement((Expression) node);
                } else {
                    var type = BMLTypeResolver.resolveBMLTypeToJavaType(ctx.expr.type);
                    block.addStatement(new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(type, varName, new NullLiteralExpr()))));
                    tryBlock.addStatement(new AssignExpr(new NameExpr(varName), (Expression) node, AssignExpr.Operator.ASSIGN));
                }

                block.addStatement(new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), varName + "Code", new IntegerLiteralExpr("200")))));

                var catchBlock = new BlockStmt();
                var catchClause = new CatchClause(new Parameter(StaticJavaParser.parseType("ApiException"), "e"), catchBlock);
                var catchClauseAssignExpr = new AssignExpr(new NameExpr(varName + "Code"),
                        new MethodCallExpr(new NameExpr("e"), "getCode", new NodeList<>()), AssignExpr.Operator.ASSIGN);
                catchBlock.addStatement(catchClauseAssignExpr);

                var tryStmt = new TryStmt(tryBlock, new NodeList<>(catchClause), null);
                block.addStatement(tryStmt);

                return block;
            } else {
                return new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), ctx.name.getText(), (Expression) node)));
            }
        } else {
            var symbol = globalScope.getSymbol(ctx.name.getText());
            var op = switch (ctx.op.getType()) {
                case BMLParser.ADD_ASSIGN -> AssignExpr.Operator.PLUS;
                case BMLParser.SUB_ASSIGN -> AssignExpr.Operator.MINUS;
                case BMLParser.MUL_ASSIGN -> AssignExpr.Operator.MULTIPLY;
                case BMLParser.DIV_ASSIGN -> AssignExpr.Operator.DIVIDE;
                case BMLParser.MOD_ASSIGN -> AssignExpr.Operator.REMAINDER;
                default -> throw new IllegalStateException("Unexpected value: " + ctx.op.getType());
            };
            if (symbol != null) {
                var generator = GeneratorRegistry.getGeneratorForType(ctx.expr.type);
                //noinspection OptionalGetWithoutIsPresent -> checked by above switch
                return generator.generateArithmeticAssignmentToGlobal(ctx, op.toBinaryOperator().get(), this);
            } else {
                // TODO: What about reassignments?

                if (ctx.op.getType() == BMLParser.ADD_ASSIGN) {
                    var generator = GeneratorRegistry.getGeneratorForType(ctx.expr.type);
                    return generator.generateAddAssignment(ctx, this);
                } else {
                    return new AssignExpr(new NameExpr(ctx.name.getText()), (Expression) visit(ctx.expr), op);
                }
            }
        }
    }

    @Override
    public Node visitExpression(BMLParser.ExpressionContext ctx) {
        if (ctx.atom() != null) {
            return visit(ctx.atom());
        } else if (ctx.op != null) {
            return switch (ctx.op.getType()) {
                case BMLParser.LBRACE -> new EnclosedExpr((Expression) visit(ctx.expr));

                case BMLParser.DOT -> {
                    Generator generator = GeneratorRegistry.getGeneratorForType(ctx.expr.type);
                    if (ctx.Identifier() != null) {
                        yield generator.generateFieldAccess((Expression) visit(ctx.expr), ctx.Identifier());
                    } else { // functionCall
                        yield generator.generateFunctionCall((Expression) visit(ctx.expr), ctx.functionCall(), this);
                    }
                }

                case BMLParser.LBRACK -> new MethodCallExpr((Expression) visit(ctx.expr), "get",
                        new NodeList<>((Expression) visit(ctx.index)));

                case BMLParser.BANG ->
                        new UnaryExpr((Expression) visit(ctx.expr), UnaryExpr.Operator.LOGICAL_COMPLEMENT);

                case BMLParser.LT, BMLParser.LE, BMLParser.GT, BMLParser.GE, BMLParser.EQUAL, BMLParser.NOTEQUAL,
                        BMLParser.ADD, BMLParser.SUB, BMLParser.MUL, BMLParser.DIV, BMLParser.MOD -> {
                    if (ctx.left.type.equals(TypeRegistry.resolveType(BuiltinType.STRING))) {
                        var methodCallExpr = new MethodCallExpr((Expression) visit(ctx.left), "equals", new NodeList<>((Expression) visit(ctx.right)));
                        if (ctx.op.getType() == BMLParser.EQUAL) {
                            yield methodCallExpr;
                        } else if (ctx.op.getType() == BMLParser.NOTEQUAL) {
                            yield new UnaryExpr(methodCallExpr, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
                        }
                    }

                    //noinspection OptionalGetWithoutIsPresent -> Our operators are a subset of Java's, so they exist
                    yield new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                            Arrays.stream(BinaryExpr.Operator.values())
                                    .filter(op -> op.asString().equals(ctx.op.getText()))
                                    .findAny()
                                    .get());
                }

                case BMLParser.AND -> new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                        BinaryExpr.Operator.AND);

                case BMLParser.OR -> new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                        BinaryExpr.Operator.OR);

                case BMLParser.QUESTION ->
                        new ConditionalExpr((Expression) visit(ctx.cond), (Expression) visit(ctx.thenExpr),
                                (Expression) visit(ctx.elseExpr));

                // This should never happen
                default -> throw new IllegalStateException("Unexpected ctx.op: %s\nContext: %s".formatted(ctx.op, ctx));
            };
        } else if (ctx.functionCall() != null) {
            if (ctx.functionCall().functionName.getText().equals("send")
                    || ctx.functionCall().functionName.getText().equals("string")
                    || ctx.functionCall().functionName.getText().equals("number")) {
                var generator = GeneratorRegistry.getGeneratorForFunctionName(ctx.functionCall().functionName.getText());
                return generator.generateFunctionCall(null, ctx.functionCall(), this);
            } else {
                return visit(ctx.functionCall());
            }
        } else { // Initializers
            return visit(ctx.initializer());
        }
    }

    @Override
    public Node visitAtom(BMLParser.AtomContext ctx) {
        var atom = ctx.token.getText();
        return switch (ctx.token.getType()) {
            case BMLParser.IntegerLiteral -> new IntegerLiteralExpr(atom);
            case BMLParser.FloatingPointLiteral -> new DoubleLiteralExpr(atom);
            case BMLParser.StringLiteral -> new StringLiteralExpr(atom.substring(1, atom.length() - 1));
            case BMLParser.BooleanLiteral -> new BooleanLiteralExpr(Boolean.parseBoolean(atom));
            case BMLParser.Identifier -> {
                // Check global scope
                var symbol = (VariableSymbol) globalScope.getSymbol(atom);
                if (symbol != null
                        && (symbol.getType().equals(TypeRegistry.resolveType(BuiltinType.BOOLEAN))
                        || symbol.getType().equals(TypeRegistry.resolveType(BuiltinType.NUMBER)))) {
                    // We have a global variable -> needs thread-safety
                    yield new MethodCallExpr(new NameExpr(atom), "getAcquire");
                }

                // Check dialogue scope, not function scope, only "global" dialogue scope
                symbol = (VariableSymbol) dialogueScope.getSymbol(atom);
                if (symbol != null) {
                    yield new MethodCallExpr(new NameExpr("dialogueAutomaton"), "get" + StringUtils.capitalize(atom));
                }

                symbol = (VariableSymbol) currentScope.resolve(atom);
                yield GeneratorRegistry.getGeneratorForType(symbol.getType()).generateNameExpr(ctx);
            }
            // This should never happen
            default -> throw new IllegalStateException("Unknown token was parsed: %s\nContext: %s".formatted(atom, ctx));
        };
    }

    @Override
    public Node visitFunctionCall(BMLParser.FunctionCallContext ctx) {
        // TODO: These calls can only be STDLIB calls
        return new MethodCallExpr(ctx.functionName.getText());
    }

    @Override
    public Node visitMapInitializer(BMLParser.MapInitializerContext ctx) {
        return GeneratorRegistry.getGeneratorForType(ctx.type).generateInitializer(ctx, this);
    }

    @Override
    public Node visitListInitializer(BMLParser.ListInitializerContext ctx) {
        return GeneratorRegistry.getGeneratorForType(ctx.type).generateInitializer(ctx, this);
    }

    @Override
    public Node visitDialogueAutomaton(BMLParser.DialogueAutomatonContext ctx) {
        dialogueScope = ctx.scope;
        pushScope(dialogueScope);
        var childNode = super.visitDialogueAutomaton(ctx);
        popScope();
        dialogueScope = new BlockScope(null);
        return childNode;
    }

    @Override
    public Node visitDialogueHead(BMLParser.DialogueHeadContext ctx) {
        return super.visitDialogueHead(ctx);
    }

    @Override
    public Node visitDialogueBody(BMLParser.DialogueBodyContext ctx) {
        var dialogueHeadContext = ((BMLParser.DialogueAutomatonContext) ctx.parent).head;
        var newDialogueClassName = "%sDialogueAutomaton".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));
        var newActionsClassName = "%sActions".formatted(StringUtils.capitalize(dialogueHeadContext.name.getText()));

        if (!ctx.dialogueFunctionDefinition().isEmpty()) {
            Utils.readAndWriteClass(botOutputPath + "dialogue", newActionsClassName, clazz -> {
                classStack.push(clazz);
                for (var c : ctx.dialogueFunctionDefinition()) {
                    var actionMethod = clazz.addMethod(c.functionDefinition().head.functionName.getText(),
                            Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                    actionMethod.addParameter("Context", "context");
                    //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
                    var compilationUnit = clazz.findCompilationUnit().get();
                    compilationUnit.addImport(Utils.renameImport(Context.class, outputPackage), false, false);
                    actionMethod.setBody((BlockStmt) visit(c.functionDefinition().body));
                }
                classStack.pop();
            });
        }

        if (!ctx.dialogueAssignment().isEmpty()) {
            Utils.readAndWriteClass(botOutputPath + "dialogue", newDialogueClassName, clazz -> {
                classStack.push(clazz);
                ctx.dialogueAssignment().forEach(c -> {
                    if (c.assignment().expr.type.equals(TypeRegistry.resolveType(BuiltinType.STATE))) {
                        visit(c);
                    } else {
                        var field = clazz.addFieldWithInitializer(BMLTypeResolver.resolveBMLTypeToJavaType(c.assignment().expr.type),
                                c.assignment().name.getText(), (Expression) visit(c.assignment().expr),
                                Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                        field.createGetter();
                    }
                });
                classStack.pop();
            });
        }

        for (var automatonTransition : ctx.automatonTransitions()) {
            visit(automatonTransition);
        }

        return null;
    }

    @Override
    public Node visitDialogueAssignment(BMLParser.DialogueAssignmentContext ctx) {
        var childNode = super.visitDialogueAssignment(ctx);

        if (ctx.assignment().expr.type.equals(TypeRegistry.resolveType(BuiltinType.STATE))) {
            // Create a class
            CompilationUnit c = new CompilationUnit();
            var className = StringUtils.capitalize(ctx.assignment().name.getText()) + "State";
            var clazz = c.addClass(className);
            clazz.addExtendedType(State.class);

            var stateType = (BMLState) ctx.assignment().expr.type;

            // Add field to store & set intent
            clazz.addField(DialogueAutomatonTemplate.class, "dialogueAutomaton", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
            var constructorBody = new BlockStmt().addStatement(new FieldAccessExpr(new NameExpr("this"), "dialogueAutomaton"))
                    .addStatement(new AssignExpr(new FieldAccessExpr(new NameExpr("super"), "intent"),
                            new StringLiteralExpr(stateType.getIntent()), AssignExpr.Operator.ASSIGN));
            clazz.addConstructor()
                    .setParameters(new NodeList<>(new Parameter(StaticJavaParser.parseType(DialogueAutomatonTemplate.class.getSimpleName()), "dialogueAutomaton")))
                    .setBody(constructorBody);

            // Set action
            var actionMethod = clazz.addMethod("action", Modifier.Keyword.PUBLIC);
            actionMethod.addAnnotation(Override.class);
            actionMethod.addParameter("Context", "context");
            //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
            var compilationUnit = clazz.findCompilationUnit().get();
            compilationUnit.addImport(Utils.renameImport(Context.class, outputPackage), false, false);

            var block = switch (stateType.getActionType().getClass().getAnnotation(BMLType.class).name()) {
                case STRING ->
                        new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                                new NodeList<>(new NameExpr("context"), (StringLiteralExpr) visit(stateType.getAction()))));
                case LIST -> {
                    var randomType = StaticJavaParser.parseClassOrInterfaceType("Random");
                    clazz.addFieldWithInitializer(Random.class, "random",
                            new ObjectCreationExpr(null, randomType, new NodeList<>()), Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

                    var getRandomIntStmt = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), "nextMessage",
                            new MethodCallExpr(new NameExpr("random"), "nextInt", new NodeList<>(new IntegerLiteralExpr("3")))));

                    var getRandomListEntry = new MethodCallExpr((Expression) visit(stateType.getAction()), "get",
                            new NodeList<>(new NameExpr("nextMessage")));

                    var sendMessageStmt = new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                            new NodeList<>(new NameExpr("context"), getRandomListEntry));

                    yield new BlockStmt().addStatement(getRandomIntStmt).addStatement(sendMessageStmt);
                }
                case FUNCTION -> {
                    //((String) stateType.getAction()); // Function name
                    // TODO
                    yield null;

                }
                default -> new BlockStmt();
            };

            actionMethod.setBody(block);

            // Create file at desired destination
            writeClass("dialogue/states", className);
        }

        return childNode;
    }

    @Override
    public Node visitAutomatonTransitions(BMLParser.AutomatonTransitionsContext ctx) {
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

            } else if (child instanceof BMLParser.TransitionInitializerContext) {
                // TODO: Unpack what is returned and add to target states of first state
                var states = visit(child);
            }
        }

        // Add body to init method
//        Utils.readAndWriteClass("dialogue", "DialogueAutomaton", clazz -> {
//            clazz.getMethodsByName("init").get(0).setBody(initBlock);
//        });

        return super.visitAutomatonTransitions(ctx);
    }

    @Override
    public Node visitTransitionInitializer(BMLParser.TransitionInitializerContext ctx) {
        return super.visitTransitionInitializer(ctx);
    }

    private void writeClass(String path, String fileName) {

    }
}
