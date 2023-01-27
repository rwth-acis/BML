package i5.bml.transpiler.generators.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.symbols.BlockScope;
import i5.bml.parser.types.components.primitives.BMLString;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.config.BotConfig;
import i5.bml.transpiler.bot.dialogue.ActionsTemplate;
import i5.bml.transpiler.bot.dialogue.DialogueAutomatonTemplate;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.GeneratorRegistry;
import i5.bml.transpiler.generators.dialogue.DialogueAutomatonGenerator;
import i5.bml.transpiler.generators.types.BMLTypeResolver;
import i5.bml.transpiler.utils.IOUtil;
import i5.bml.transpiler.utils.PrinterUtil;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;
import org.stringtemplate.v4.ST;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaTreeGenerator extends BMLBaseVisitor<Node> {

    private final String botOutputPath;

    private final String outputPackage;

    private final ST gradleFile;

    private Scope currentScope;

    private Scope globalScope;

    private Scope dialogueScope = new BlockScope(null);

    private final Deque<ClassOrInterfaceDeclaration> classStack = new ArrayDeque<>();

    private boolean wrapAssignmentInTryStmt = false;

    public JavaTreeGenerator(String botOutputPath, String outputPackage, ST gradleFile) {
        this.botOutputPath = botOutputPath;
        this.outputPackage = outputPackage;
        this.gradleFile = gradleFile;
    }

    private void pushScope(Scope s) {
        currentScope = s;
    }

    private void popScope() {
        currentScope = currentScope.getEnclosingScope();
    }

    public String botOutputPath() {
        return botOutputPath;
    }

    public String outputPackage() {
        return outputPackage;
    }

    public ST gradleFile() {
        return gradleFile;
    }

    public Deque<ClassOrInterfaceDeclaration> classStack() {
        return classStack;
    }

    public ClassOrInterfaceDeclaration currentClass() {
        return classStack.peek();
    }

    public void wrapAssignmentInTryStmt(boolean wrapAssignmentInTryStmt) {
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
        PrinterUtil.readAndWriteClass(botOutputPath, BotConfig.class, clazz -> {
            for (var pair : ctx.params.elementExpressionPair()) {
                var type = BMLTypeResolver.resolveBMLTypeToJavaType(pair.expr.type);
                var name = pair.name.getText().toUpperCase();
                clazz.addFieldWithInitializer(type, name, (Expression) visit(pair.expr),
                        Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
            }
        });

        return null;
    }

    /**
     * Visits the body of the bot and generates code for the various components, dialogues and functions defined within it.
     * Populates the {@link i5.bml.transpiler.bot.threads.Session} class with required dialogues and iterates over child nodes in a procedural manner
     * (i.e., as they appear in the source text).
     * Deletes dialogue templates if at least one dialogue is present and writes back the generated {@link ComponentRegistry} class.
     *
     * @param ctx The context of the bot body being visited.
     * @return null since all children have been visited and there is nothing to return.
     */
    @Override
    public Node visitBotBody(BMLParser.BotBodyContext ctx) {
        var componentRegistryClass = PrinterUtil.readClass(botOutputPath, ComponentRegistry.class);

        if (!ctx.dialogueAutomaton().isEmpty()) {
            // We populate the Session class with required dialogues (we can pick any dialogue from scope)
            var dialogueType = ((VariableSymbol) currentScope.resolve(ctx.dialogueAutomaton(0).head.name.getText())).getType();
            GeneratorRegistry.generatorForType(dialogueType).generateComponent(null, this);
        }

        // Iterate over child nodes in body in procedural manner, i.e., as they appear in the source text
        for (var child : ctx.children) {
            if (child.getText().equals("{") || child.getText().equals("}")) {
                continue;
            }

            if (child instanceof BMLParser.ComponentContext componentContext) {
                classStack.push(componentRegistryClass);
                visit(componentContext);
                classStack.pop();
            } else if (child instanceof BMLParser.DialogueAutomatonContext automatonContext) {
                visit(automatonContext);
            } else if (child instanceof BMLParser.FunctionDefinitionContext functionContext) {
                for (var annotationContext : functionContext.annotation()) {
                    var generator = GeneratorRegistry.generatorForType(annotationContext.type);
                    generator.populateClassWithFunction(functionContext, annotationContext, this);
                }
            }
        }

        // Delete dialogue templates, if we have at least one dialogue
        if (!ctx.dialogueAutomaton().isEmpty()) {
            IOUtil.forceDeleteFile(botOutputPath, DialogueAutomatonTemplate.class);
            IOUtil.forceDeleteFile(botOutputPath, ActionsTemplate.class);
        }

        // Write back the ComponentRegistry class
        PrinterUtil.writeClass(botOutputPath, ComponentRegistry.class, componentRegistryClass);

        // We have visited all children already, and we have nothing to return
        return null;
    }

    @Override
    public Node visitComponent(BMLParser.ComponentContext ctx) {
        GeneratorRegistry.generatorForType(ctx.type).generateComponent(ctx, this);
        return null;
    }

    @Override
    public Node visitFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        pushScope(ctx.scope);
        var result = super.visitFunctionDefinition(ctx);
        popScope();
        return result;
    }

    /**
     * Visit a statement context and generate code for it.
     * If the statement has an operator, return a new {@link BreakStmt} object.
     * If the statement is an {@link IfStmt}, {@link ForEachStmt} or {@link BlockStmt}, push the current scope onto the stack,
     * visit the statement and pop the scope off the stack.
     * If the statement is an expression, return a new {@link ExpressionStmt} object with the expression generated from visiting the statement.
     * Otherwise, visit the statement and returns the generated code.
     *
     * @param ctx The context of the statement being visited.
     * @return The generated code for the statement.
     */
    @Override
    public Node visitStatement(BMLParser.StatementContext ctx) {
        if (ctx.op != null) {
            return new BreakStmt();
        } else if (ctx.ifStatement() != null || ctx.forEachStatement() != null || ctx.block() != null) {
            pushScope(ctx.scope);
            var node = super.visitStatement(ctx);
            popScope();
            return node;
        } else if (ctx.expr != null) {
            var node = super.visitStatement(ctx);
            if (node instanceof BlockStmt) {
                return node;
            } else {
                return new ExpressionStmt((Expression) super.visitStatement(ctx));
            }
        } else {
            return super.visitStatement(ctx);
        }
    }

    /**
     * Visit a block context and generate code for it.
     * Return a new {@link BlockStmt} object with a {@link NodeList} of statements generated from visiting each statement in the block context.
     * If a statement returns a {@link BlockStmt} object, its statements are flattened and added to the {@link NodeList}.
     * If a statement is not a {@link Statement}, it is wrapped in an {@link ExpressionStmt}.
     *
     * @param ctx The context of the block being visited.
     * @return The generated code for the block as a {@link BlockStmt} object.
     */
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
        BlockStmt body = (BlockStmt) visit(ctx.forEachBody());
        var iterable = visit(ctx.expr);

        if (iterable instanceof ForStmt forStmt) {
            forStmt.setBody(body);
            return forStmt;
        } else {
            if (ctx.comma == null) { // List
                variable = new VariableDeclarationExpr(new VarType(), ctx.Identifier(0).getText());
            } else { // Map
                var mapEntryVarName = "entry";
                variable = new VariableDeclarationExpr(new VarType(), mapEntryVarName);

                body.addStatement(0, new VariableDeclarationExpr(new VariableDeclarator(new VarType(),
                        ctx.Identifier(0).getText(), new MethodCallExpr(new NameExpr(mapEntryVarName), "getKey"))));
                body.addStatement(0, new VariableDeclarationExpr(new VariableDeclarator(new VarType(),
                        ctx.Identifier(1).getText(), new MethodCallExpr(new NameExpr(mapEntryVarName), "getValue"))));

                iterable = new MethodCallExpr((Expression) iterable, "entrySet");
            }

            return new ForEachStmt(variable, (Expression) iterable, body);
        }
    }

    /**
     * TODO
     *
     * @param ctx the parse tree
     * @return
     */
    @Override
    public Node visitAssignment(BMLParser.AssignmentContext ctx) {
        var name = ctx.name.getText();
        if (ctx.op.getType() == BMLParser.ASSIGN) {
            var node = visit(ctx.expr);
            if (wrapAssignmentInTryStmt) {
                // Reset
                wrapAssignmentInTryStmt = false;

                var block = new BlockStmt();
                var tryBlock = new BlockStmt();

                if (ctx.expr.type.getName().equals("empty")) {
                    tryBlock.addStatement((Expression) node);
                } else {
                    var type = GeneratorRegistry.generatorForType(ctx.expr.type).generateVariableType(ctx.expr.type, this);
                    block.addStatement(new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(type, name, new NullLiteralExpr()))));
                    tryBlock.addStatement(new AssignExpr(new NameExpr(name), (Expression) node, AssignExpr.Operator.ASSIGN));
                }

                block.addStatement(new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), name + "Code", new IntegerLiteralExpr("200")))));

                var catchBlock = new BlockStmt();
                var catchClause = new CatchClause(new Parameter(StaticJavaParser.parseType("ApiException"), "e"), catchBlock);
                var catchClauseAssignExpr = new AssignExpr(new NameExpr(name + "Code"),
                        new MethodCallExpr(new NameExpr("e"), "getCode", new NodeList<>()), AssignExpr.Operator.ASSIGN);
                catchBlock.addStatement(catchClauseAssignExpr);

                var tryStmt = new TryStmt(tryBlock, new NodeList<>(catchClause), null);
                block.addStatement(tryStmt);

                return block;
            } else {
                if (ctx.isReassignment) {
                    return new ExpressionStmt(new AssignExpr(new NameExpr(name), (Expression) node, AssignExpr.Operator.ASSIGN));
                } else {
                    return new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), name, (Expression) node)));
                }
            }
        } else {
            var symbol = globalScope.getSymbol(name);
            var op = switch (ctx.op.getType()) {
                case BMLParser.ADD_ASSIGN -> AssignExpr.Operator.PLUS;
                case BMLParser.SUB_ASSIGN -> AssignExpr.Operator.MINUS;
                case BMLParser.MUL_ASSIGN -> AssignExpr.Operator.MULTIPLY;
                case BMLParser.DIV_ASSIGN -> AssignExpr.Operator.DIVIDE;
                case BMLParser.MOD_ASSIGN -> AssignExpr.Operator.REMAINDER;
                default -> throw new IllegalStateException("Unexpected value: " + ctx.op.getType());
            };
            if (symbol != null) {
                var generator = GeneratorRegistry.generatorForType(ctx.expr.type);
                //noinspection OptionalGetWithoutIsPresent -> checked by above switch
                return generator.generateArithmeticAssignmentToGlobal(ctx, op.toBinaryOperator().get(), this);
            } else {
                // TODO: What about reassignments?

                if (ctx.op.getType() == BMLParser.ADD_ASSIGN) {
                    var generator = GeneratorRegistry.generatorForType(ctx.expr.type);
                    return generator.generateAddAssignment(ctx, this);
                } else {
                    return new AssignExpr(new NameExpr(name), (Expression) visit(ctx.expr), op);
                }
            }
        }
    }

    /**
     * Visits an expression and generates the appropriate code based on the type of expression.
     * If the expression is an atom, visit the atom.
     * If the expression has an operator, generates code for the operation based on the operator type.
     * If the expression is a function call, visit the function call.
     * If the expression is an initializer, visit the initializer.
     *
     * @param ctx The context of the expression being visited.
     * @return Node representing the generated code for the expression.
     */
    @Override
    public Node visitExpression(BMLParser.ExpressionContext ctx) {
        if (ctx.atom() != null) {
            return visit(ctx.atom());
        } else if (ctx.op != null) {
            return switch (ctx.op.getType()) {
                case BMLParser.LPAREN -> new EnclosedExpr((Expression) visit(ctx.expr));

                case BMLParser.DOT -> {
                    Generator generator = GeneratorRegistry.generatorForType(ctx.expr.type);
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
                    if (ctx.left.type instanceof BMLString) {
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
            return visit(ctx.functionCall());
        } else { // Initializers
            return visit(ctx.initializer());
        }
    }

    /**
     * TODO
     *
     * @param ctx the parse tree
     * @return
     */
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
                if (symbol != null) {
                    // We have a global variable -> needs thread-safety
                    yield GeneratorRegistry.generatorForType(symbol.getType()).generateGlobalNameExpr(ctx);
                }

                // Check dialogue scope, not function scope, only "global" dialogue scope
                symbol = (VariableSymbol) dialogueScope.getSymbol(atom);
                if (symbol != null && !currentClass().getImplementedTypes().isEmpty()) {
                    yield new NameExpr(atom);
                }

                symbol = (VariableSymbol) currentScope.resolve(atom);
                yield GeneratorRegistry.generatorForType(symbol.getType()).generateNameExpr(ctx);
            }
            // This should never happen
            default ->
                    throw new IllegalStateException("Unknown token was parsed: %s\nContext: %s".formatted(atom, ctx));
        };
    }

    @Override
    public Node visitFunctionCall(BMLParser.FunctionCallContext ctx) {
        var generator = GeneratorRegistry.generatorForFunctionName(ctx.functionName.getText());
        return generator.generateFunctionCall(null, ctx, this);
    }

    @Override
    public Node visitMapInitializer(BMLParser.MapInitializerContext ctx) {
        return GeneratorRegistry.generatorForType(ctx.type).generateInitializer(ctx, this);
    }

    @Override
    public Node visitListInitializer(BMLParser.ListInitializerContext ctx) {
        return GeneratorRegistry.generatorForType(ctx.type).generateInitializer(ctx, this);
    }

    /**
     * Visit a dialogue automaton and generate code for it.
     * Set the dialogue scope to the current context's scope, push the scope onto the stack, and visit the dialogue head.
     * Initialize a {@link DialogueAutomatonGenerator}, visit the dialogue body with the current scope, and pop the scope off the stack.
     * <p>
     * We outsourced the generation of a dialogue automaton into a single class to keep the generator clear.
     *
     * @param ctx The context of the dialogue automaton being visited.
     * @return null since all children have been visited and there is nothing to return.
     */
    @Override
    public Node visitDialogueAutomaton(BMLParser.DialogueAutomatonContext ctx) {
        dialogueScope = ctx.scope;
        pushScope(dialogueScope);

        DialogueAutomatonGenerator dialogueAutomatonGenerator = new DialogueAutomatonGenerator(this);
        visitDialogueHead(ctx.head);
        dialogueAutomatonGenerator.init(ctx.head);
        dialogueAutomatonGenerator.visitDialogueBody(ctx.body, currentScope);

        popScope();
        dialogueScope = new BlockScope(null);

        return null;
    }
}
