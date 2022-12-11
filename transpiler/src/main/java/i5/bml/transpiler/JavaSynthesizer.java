package i5.bml.transpiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.GeneratorRegistry;
import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaSynthesizer extends BMLBaseVisitor<Node> {

    private String botOutputPath = "transpiler/src/main/java/i5/bml/transpiler/bot/";

    private Scope currentScope;

    private Scope globalScope;

    private void pushScope(Scope s) {
        currentScope = s;
    }

    private void popScope() {
        currentScope = currentScope.getEnclosingScope();
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
        try {
            var botConfig = new File(botOutputPath + "BotConfig.java");
            CompilationUnit c = StaticJavaParser.parse(botConfig);
            //noinspection OptionalGetWithoutIsPresent -> We can assume that the class is present
            var clazz = c.getClassByName("BotConfig").get();
            System.out.println(c);

            for (var pair : ctx.params.elementExpressionPair()) {
                var type = BMLTypeResolver.resolveBMLTypeToJavaType(pair.expr.type);
                var name = pair.name.getText().toUpperCase();
                clazz.addFieldWithInitializer(type, name, (Expression) visit(pair.expr),
                        Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
            }

            // Write back to botConfig
            Files.write(botConfig.toPath(), c.toString().getBytes());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s in %s".formatted("BotConfig.java", botOutputPath));
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s%s: %s"
                    .formatted(botOutputPath, "BotConfig.java", e.getMessage()));
        }

        return super.visitBotHead(ctx);
    }

    @Override
    public Node visitElementExpressionPairList(BMLParser.ElementExpressionPairListContext ctx) {
        pushScope(ctx.scope);
        var result = super.visitElementExpressionPairList(ctx);
        popScope();
        return result;
    }

    @Override
    public Node visitBotBody(BMLParser.BotBodyContext ctx) {
        // TODO:
        ctx.component();

        return super.visitBotBody(ctx);
    }

    @Override
    public Node visitComponent(BMLParser.ComponentContext ctx) {
        return GeneratorRegistry.getGeneratorForType(ctx.type).generateComponent(ctx, this);
    }

    @Override
    public Node visitFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        // TODO: Make distinction for different Annotations

        pushScope(ctx.scope);
        var result = super.visitFunctionDefinition(ctx);
        popScope();
        return result;
    }

    @Override
    public Node visitStatement(BMLParser.StatementContext ctx) {
        pushScope(ctx.scope);
        var result = super.visitStatement(ctx);
        popScope();
        return result;
    }

    @Override
    public Node visitBlock(BMLParser.BlockContext ctx) {
        return new BlockStmt(ctx.statement().stream()
                .map(statementContext -> {
                    var node = visit(statementContext);
                    return !(node instanceof Statement) ? new ExpressionStmt((Expression) node) : (Statement) node;
                })
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
        if (ctx.comma == null) { // List
            variable = new VariableDeclarationExpr(new VarType(), ctx.Identifier(0).getText());
        } else { // Map
            var mapEntryVarName = "e";
            variable = new VariableDeclarationExpr(new VarType(), mapEntryVarName);

            forEachBody.addStatement(0, new VariableDeclarationExpr(new VariableDeclarator(new VarType(),
                    ctx.Identifier(0).getText(), new MethodCallExpr(new NameExpr(mapEntryVarName), "getKey"))));
            forEachBody.addStatement(0, new VariableDeclarationExpr(new VariableDeclarator(new VarType(),
                    ctx.Identifier(1).getText(), new MethodCallExpr(new NameExpr(mapEntryVarName), "getValue"))));
        }

        return new ForEachStmt(variable, (Expression) visit(ctx.expr), forEachBody);
    }

    @Override
    public Node visitAssignment(BMLParser.AssignmentContext ctx) {
        if (ctx.op.getType() == BMLParser.ASSIGN) {
            var type = BMLTypeResolver.resolveBMLTypeToJavaType(ctx.expr.type);
            return new ExpressionStmt(new VariableDeclarationExpr(new VariableDeclarator(type, ctx.name.getText(), (Expression) visit(ctx.expr))));
        } else {
            switch (ctx.op.getType()) {
                // TODO
            }

            return null;
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
                        yield generator.generateFunctionCall(ctx.functionCall(), this);
                    }
                }

                case BMLParser.LBRACK -> new MethodCallExpr((Expression) visit(ctx.expr), "get",
                        new NodeList<>((Expression) visit(ctx.index)));

                case BMLParser.BANG ->
                        new UnaryExpr((Expression) visit(ctx.expr), UnaryExpr.Operator.LOGICAL_COMPLEMENT);

                case BMLParser.LT, BMLParser.LE, BMLParser.GT, BMLParser.GE, BMLParser.EQUAL, BMLParser.NOTEQUAL,
                        BMLParser.ADD, BMLParser.SUB, BMLParser.MUL, BMLParser.DIV, BMLParser.MOD ->
                        //noinspection OptionalGetWithoutIsPresent -> Our operators are a subset of Java's, so they exist
                        new BinaryExpr((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                                Arrays.stream(BinaryExpr.Operator.values())
                                        .filter(op -> op.asString().equals(ctx.op.getText()))
                                        .findAny()
                                        .get());

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

    @Override
    public Node visitAtom(BMLParser.AtomContext ctx) {
        var atom = ctx.token.getText();
        return switch (ctx.token.getType()) {
            case BMLParser.IntegerLiteral -> new IntegerLiteralExpr(atom);
            case BMLParser.FloatingPointLiteral -> new DoubleLiteralExpr(atom);
            case BMLParser.StringLiteral -> new StringLiteralExpr(atom.substring(1, atom.length() - 1));
            case BMLParser.BooleanLiteral -> new BooleanLiteralExpr(Boolean.parseBoolean(atom));
            case BMLParser.Identifier -> {
                var symbol = ((VariableSymbol) globalScope.getSymbol(atom));
                if (symbol != null
                        && (symbol.getType().equals(TypeRegistry.resolveType(BuiltinType.BOOLEAN))
                            || symbol.getType().equals(TypeRegistry.resolveType(BuiltinType.NUMBER)))) {
                    // We have a global variable -> needs thread-safety
                    yield new MethodCallExpr(new NameExpr(atom), "getAcquire");
                } else {
                    yield new NameExpr(atom);
                }
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
        var elementExpressionPairList = ctx.elementExpressionPairList();
        if (elementExpressionPairList != null) {
            var arguments = elementExpressionPairList.elementExpressionPair().stream()
                    .flatMap(p -> Stream.of(new StringLiteralExpr(p.name.getText()), (Expression) visit(p.expr)))
                    .collect(Collectors.toCollection(NodeList::new));
            return new MethodCallExpr(new NameExpr("Map"), "of", arguments);
        } else {
            return new MethodCallExpr(new NameExpr("Map"), "of");
        }
    }

    @Override
    public Node visitListInitializer(BMLParser.ListInitializerContext ctx) {
        var arguments = ctx.expression().stream()
                .map(e -> (Expression) visit(e))
                .collect(Collectors.toCollection(NodeList::new));
        return new MethodCallExpr(new NameExpr("List"), new SimpleName("of"), arguments);
    }

    @Override
    public Node visitDialogueAutomaton(BMLParser.DialogueAutomatonContext ctx) {
        return super.visitDialogueAutomaton(ctx);
    }

    @Override
    public Node visitDialogueHead(BMLParser.DialogueHeadContext ctx) {
        return super.visitDialogueHead(ctx);
    }

    @Override
    public Node visitDialogueBody(BMLParser.DialogueBodyContext ctx) {
        return super.visitDialogueBody(ctx);
    }

    @Override
    public Node visitAutomatonTransitions(BMLParser.AutomatonTransitionsContext ctx) {
        return super.visitAutomatonTransitions(ctx);
    }

    @Override
    public Node visitTransitionInitializer(BMLParser.TransitionInitializerContext ctx) {
        return super.visitTransitionInitializer(ctx);
    }
}
