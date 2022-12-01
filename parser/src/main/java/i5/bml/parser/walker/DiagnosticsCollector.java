package i5.bml.parser.walker;

import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.symbols.BlockScope;
import i5.bml.parser.types.*;
import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static i5.bml.parser.errors.ParserError.*;

public class DiagnosticsCollector extends BMLBaseListener {

    private final List<Diagnostic> collectedDiagnostics = new ArrayList<>();

    protected Scope currentScope;

    public List<Diagnostic> getCollectedDiagnostics() {
        return collectedDiagnostics;
    }

    /*
     * Symbol Table & Scope Generator
     */
    @Override
    public void enterBotDeclaration(BMLParser.BotDeclarationContext ctx) {
        GlobalScope g = new GlobalScope(null);
        ctx.scope = g;
        pushScope(g);
    }

    @Override
    public void exitBotDeclaration(BMLParser.BotDeclarationContext ctx) {
        popScope();
    }

    @Override
    public void enterBotHead(BMLParser.BotHeadContext ctx) {
        if (ctx.name != null) {
            checkAlreadyDefinedElseDefine(ctx.name);
        }
    }

    @Override
    public void enterElementExpressionPairList(BMLParser.ElementExpressionPairListContext ctx) {
        Scope s = new BlockScope(currentScope);
        ctx.scope = s;
        pushScope(s);
    }

    @Override
    public void exitElementExpressionPairList(BMLParser.ElementExpressionPairListContext ctx) {
        popScope();
    }

    @Override
    public void enterComponent(BMLParser.ComponentContext ctx) {
        checkAlreadyDefinedElseDefine(ctx.name);
    }

    @Override
    public void enterFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        checkAlreadyDefinedElseDefine(ctx.head.functionName);

        // We still want to create a scope, even if name already exists
        Scope f = new FunctionSymbol(ctx.head.functionName.getText());
        f.setEnclosingScope(currentScope);
        ctx.scope = f;
        pushScope(f);
    }

    @Override
    public void exitFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        popScope();
    }

    @Override
    public void enterFunctionHead(BMLParser.FunctionHeadContext ctx) {
        checkAlreadyDefinedElseDefine(ctx.parameterName);
    }

    @Override
    public void enterStatement(BMLParser.StatementContext ctx) {
        // We create a scope if we have an if or foreach statement or a block
        if (ctx.ifStatement() != null || ctx.forEachStatement() != null || ctx.block() != null) {
            Scope s = new BlockScope(currentScope);
            ctx.scope = s;
            pushScope(s);
        }
        // We do not have a "block", so we check whether it's a "statement expression" (e.g., function call)
        else if (ctx.expression() != null // We have an expression
                && (ctx.expression().op == null || ctx.expression().op.getType() != BMLParser.DOT) // Expression is not using obj.foo()
                && ctx.expression().functionCall() == null) { // Expression is not a function call
            Diagnostics.addDiagnostic(collectedDiagnostics, "Not a statement", ctx.expression());
        }
    }

    @Override
    public void exitStatement(BMLParser.StatementContext ctx) {
        if (ctx.ifStatement() != null || ctx.forEachStatement() != null || ctx.block() != null) {
            popScope();
        }
    }

    @Override
    public void enterForEachStatement(BMLParser.ForEachStatementContext ctx) {
        checkAlreadyDefinedElseDefine(ctx.Identifier(0).getSymbol());
        if (ctx.comma != null) {
            checkAlreadyDefinedElseDefine(ctx.Identifier(1).getSymbol());
        }
    }

    private void pushScope(Scope s) {
        currentScope = s;
    }

    private void popScope() {
        currentScope = currentScope.getEnclosingScope();
    }

    private void checkAlreadyDefinedElseDefine(Token token) {
        var name = token.getText();

        // Check: name is already defined in scope
        if (currentScope.resolve(name) != null) {
            Diagnostics.addDiagnostic(collectedDiagnostics, ALREADY_DEFINED.format(name), token);
        } else {
            currentScope.define(new VariableSymbol(name));
        }
    }

    /*
     * Type Checking
     */
    @Override
    public void exitComponent(BMLParser.ComponentContext ctx) {
        var typeName = ctx.typeName.getText();
        var componentName = ctx.name.getText();

        // 0. Check whether type is "allowed"
        if (!TypeRegistry.isTypeBuiltin(typeName)) {
            Diagnostics.addDiagnostic(collectedDiagnostics, UNKNOWN_TYPE.format(typeName), ctx.typeName);
            return;
        }

        // 1. Create instance from blueprint
        AbstractBMLType resolvedType;
        if (TypeRegistry.isTypeComplex(typeName)) {
            resolvedType = (AbstractBMLType) TypeRegistry.resolveComplexType(typeName);
        } else {
            resolvedType = (AbstractBMLType) TypeRegistry.resolveType(typeName);
        }

        // 2. Allow resolvedType to add its desired parameters to optional and required parameter lists
        //noinspection ConstantConditions -> We know that type exists (checked by isTypeBuiltin)
        resolvedType.collectParameters();

        // 2.1 Check parameter types
        resolvedType.checkParameters(this, ctx.params);

        // 2.2 Populate annotated fields with parameters (required vs optional)
        resolvedType.populateParameters(this, ctx.params);

        var registeredType = (AbstractBMLType) TypeRegistry.resolveType(resolvedType.toString());
        if (registeredType == null) {
            // 3. Invoke initializer (e.g., fetch OpenAPI schemas from provided url parameter)
            // NOTE: We do not pass a diagnosticsCollector to this method since we want the type
            // itself to store its type initialization specific diagnostics. The reason is that
            // this branch is only visited when we encounter a new type (e.g., OpenAPI + new URL)
            resolvedType.initializeType(ctx);
            TypeRegistry.registerType(resolvedType);
        } else {
            resolvedType = registeredType;
        }

        // 5. Add collected diagnostics (either from initialization or cached)
        // See comment above, this is important for the type caching functionality
        // Whenever we have already initialized type but encountered errors, we want
        // to have them saved such that we do not have to retrieve them again
        var cachedDiagnostics = resolvedType.getCachedDiagnostics();
        for (var diagnostic : cachedDiagnostics) {
            Diagnostics.addDiagnostic(collectedDiagnostics, diagnostic, ctx);
        }

        // 6. Lastly, we set the type of the corresponding symbol
        var v = currentScope.resolve(componentName);
        ((VariableSymbol) v).setType(resolvedType);
    }

    @Override
    public void enterForEachBody(BMLParser.ForEachBodyContext ctx) {
        var forEachStmtCtx = ((BMLParser.ForEachStatementContext) ctx.parent);
        var exprType = forEachStmtCtx.expression().type;
        Type itemType;
        Type valueType = null;
        if (!(exprType instanceof BMLList) && !(exprType instanceof BMLMap)) {
            Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("List or Map", exprType),
                    forEachStmtCtx.expression());
            itemType = TypeRegistry.resolveType("Object");
        } else if (exprType instanceof BMLList) {
            itemType = ((BMLList) exprType).getItemType();
        } else { // Map
            itemType = ((BMLMap) exprType).getKeyType();
            valueType = ((BMLMap) exprType).getValueType();
        }

        // Resolve iterator variable
        var resolvedSymbol = currentScope.resolve(forEachStmtCtx.Identifier().get(0).getText());
        ((VariableSymbol) resolvedSymbol).setType(itemType);

        // Check for second iterator variable
        if (forEachStmtCtx.comma != null) {
            resolvedSymbol = currentScope.resolve(forEachStmtCtx.Identifier().get(1).getText());
            ((VariableSymbol) resolvedSymbol).setType(valueType);
        }
    }

    @Override
    public void exitAssignment(BMLParser.AssignmentContext ctx) {
        // When exiting an assignment, we can assume that the right-hand side type was computed already,
        // since it is an expression
        if (ctx.op.getText().equals("=")) {
            var name = ctx.name.getText();
            // Check: name is already defined in scope
            if (currentScope.resolve(name) != null) {
                Diagnostics.addDiagnostic(collectedDiagnostics, ALREADY_DEFINED.format(name), ctx.name);
            } else {
                VariableSymbol v = new VariableSymbol(name);
                v.setType(ctx.expression().type);
                currentScope.define(v);
            }
        } else { // Assignment operators with simultaneous arithmetic operation
            // Make sure left-hand side is defined
            var v = currentScope.resolve(ctx.name.getText());
            if (!(v instanceof VariableSymbol)) {
                Diagnostics.addDiagnostic(collectedDiagnostics, NOT_DEFINED.format(ctx.name.getText()), ctx.name);
            }

            // Type of left-hand side should already be set
            var rightType = ctx.expression().type;
            if (!(rightType instanceof BMLNumber)) {
                Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Number", rightType), ctx.name);
            }
        }
    }

    @Override
    public void exitAtom(BMLParser.AtomContext ctx) {
        ctx.type = switch (ctx.token.getType()) {
            case BMLParser.IntegerLiteral -> TypeRegistry.resolveType("Number");
            case BMLParser.FloatingPointLiteral -> TypeRegistry.resolveType("Float Number");
            case BMLParser.StringLiteral -> TypeRegistry.resolveType("String");
            case BMLParser.BooleanLiteral -> TypeRegistry.resolveType("Boolean");
            case BMLParser.Identifier -> {
                var name = ctx.Identifier().getText();
                var resolvedSymbol = currentScope.resolve(name);
                if (!(resolvedSymbol instanceof VariableSymbol)) {
                    Diagnostics.addDiagnostic(collectedDiagnostics, NOT_DEFINED.format(name), ctx.Identifier().getSymbol());
                    // We don't know the type, so we go with Object
                    yield TypeRegistry.resolveType("Object");
                } else {
                    yield ((VariableSymbol) resolvedSymbol).getType();
                }
            }
            // This should never happen
            default ->
                    throw new IllegalStateException("Unknown token was parsed: %s\nContext: %s".formatted(ctx.getText(), ctx));
        };
    }

    @Override
    public void exitExpression(BMLParser.ExpressionContext ctx) {
        if (ctx.atom() != null) {
            ctx.type = ctx.atom().type;
        } else if (ctx.op != null) {
            ctx.type = switch (ctx.op.getType()) {
                case BMLParser.LBRACE -> ctx.expr.type;
                case BMLParser.DOT -> {
                    AbstractBMLType prevType = (AbstractBMLType) ctx.expr.type;
                    var currentCtx = ctx.Identifier() != null ? ctx.Identifier() : ctx.functionCall();
                    AbstractBMLType resolvedType = (AbstractBMLType) prevType.resolveAccess(this, currentCtx);

                    if (resolvedType == null) {
                        if (ctx.Identifier() != null) {
                            Diagnostics.addDiagnostic(collectedDiagnostics,
                                    CANT_RESOLVE_IN.format(currentCtx.getText(), prevType), ctx.Identifier().getSymbol());
                        } else {
                            Diagnostics.addDiagnostic(collectedDiagnostics,
                                    CANT_RESOLVE_IN.format(currentCtx.getText(), prevType),
                                    ctx.functionCall());
                        }

                        yield TypeRegistry.resolveType("Object");
                    } else {
                        // In case of a function call, we need to unwrap the BMLFunction type to get the return type
                        if (ctx.functionCall() != null) {
                            if (resolvedType instanceof BMLFunction) {
                                // If method: check parameters -> delegate check to BMLFunction
                                resolvedType.checkParameters(this, ctx.functionCall().elementExpressionPairList());
                                yield ((BMLFunction) resolvedType).getReturnType();
                            } else {
                                yield TypeRegistry.resolveType("Object");
                            }
                        } else {
                            yield resolvedType;
                        }
                    }
                }
                case BMLParser.LBRACK -> {
                    var firstExpression = ctx.expression().get(0);
                    var firstExpressionType = firstExpression.type;
                    var secondExpression = ctx.expression().get(1);
                    var secondExpressionType = secondExpression.type;

                    if (!(firstExpressionType instanceof BMLList)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("List", firstExpressionType), firstExpression);
                        yield TypeRegistry.resolveType("Object");
                    } else if (!(secondExpressionType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Number", secondExpressionType), secondExpression);
                        yield TypeRegistry.resolveType("Object");
                    } else if (((BMLNumber) secondExpressionType).isFloatingPoint()) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(new BMLNumber(false), secondExpressionType), secondExpression);
                        yield TypeRegistry.resolveType("Object");
                    } else {
                        // Safe cast because we checked that first expression is a list
                        yield ((BMLList) firstExpressionType).getItemType();
                    }
                }
                case BMLParser.BANG -> {
                    var exprType = ctx.expr.type;
                    if (!(exprType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Boolean", exprType), ctx.expr);
                        yield TypeRegistry.resolveType("Boolean");
                    } else {
                        yield exprType;
                    }
                }
                case BMLParser.LT, BMLParser.LE, BMLParser.GT, BMLParser.GE -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!(leftType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Number", leftType), ctx.left);
                    } else if (!(rightType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Number", rightType), ctx.right);
                    }

                    yield TypeRegistry.resolveType("Boolean");
                }
                case BMLParser.EQUAL, BMLParser.NOTEQUAL -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!leftType.equals(rightType)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, INCOMPATIBLE.format(leftType, ctx.op.getText(), rightType), ctx);
                    }

                    yield TypeRegistry.resolveType("Boolean");
                }
                case BMLParser.ADD, BMLParser.SUB, BMLParser.MUL, BMLParser.DIV, BMLParser.MOD -> {
                    if (ctx.left == null) {
                        var expressionType = ctx.expr.type;
                        if (!(expressionType instanceof BMLNumber)) {
                            Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Number", expressionType), ctx.expr);
                            yield TypeRegistry.resolveType("Number");
                        } else {
                            yield expressionType;
                        }
                    } else {
                        var leftType = ctx.left.type;
                        var rightType = ctx.right.type;
                        // TODO: Allow string addition

                        if (!(leftType instanceof BMLNumber)) {
                            Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Number", leftType), ctx.left);
                            yield TypeRegistry.resolveType("Number");
                        } else if (!(rightType instanceof BMLNumber)) {
                            Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("Number", rightType), ctx.right);
                            yield TypeRegistry.resolveType("Number");
                        } else {
                            var isLeftOrRightFloat = ((BMLNumber) leftType).isFloatingPoint() || ((BMLNumber) rightType).isFloatingPoint();
                            yield TypeRegistry.resolveType(isLeftOrRightFloat ? "Float Number" : "Number");
                        }
                    }
                }
                case BMLParser.AND, BMLParser.OR -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;
                    if (!(leftType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("boolean", leftType), ctx.left);
                        yield TypeRegistry.resolveType("Boolean");
                    } else if (!(rightType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("boolean", rightType), ctx.right);
                        yield TypeRegistry.resolveType("Boolean");
                    } else {
                        yield leftType;
                    }
                }
                case BMLParser.QUESTION -> {
                    var condType = ctx.expression().get(0).type;
                    var firstType = ctx.expression().get(1).type;
                    var secondType = ctx.expression().get(2).type;
                    if (!(condType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format("boolean", condType), ctx.expression().get(0));
                        yield TypeRegistry.resolveType("Object");
                    } else if (!firstType.equals(secondType)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, INCOMPATIBLE.format(condType + " ? " + firstType, ":", secondType), ctx);
                        yield TypeRegistry.resolveType("Object");
                    } else {
                        yield firstType;
                    }
                }
                // This should never happen
                default -> throw new IllegalStateException("Unexpected ctx.op: %s\nContext: %s".formatted(ctx.op, ctx));
            };
        } else if (ctx.functionCall() != null) {
            var name = ctx.functionCall().functionName.getText();
            var symbol = currentScope.resolve(name);
            if (symbol == null) {
                Diagnostics.addDiagnostic(collectedDiagnostics, NOT_DEFINED.format(name), ctx.functionCall().functionName);
                ctx.type = TypeRegistry.resolveType("Object");
            } else {
                ctx.type = ((BMLFunction) ((TypedSymbol) symbol).getType()).getReturnType();
            }
        } else { // Initializers
            handleInitializers(ctx);
        }
    }

    private void handleInitializers(BMLParser.ExpressionContext ctx) {
        if (ctx.initializer().mapInitializer() != null) {
            var elementExpressionPairs = ctx.initializer().mapInitializer().elementExpressionPairList().elementExpressionPair();
            if (elementExpressionPairs.isEmpty()) {
                Type mapType = new BMLMap(TypeRegistry.resolveType("Object"), TypeRegistry.resolveType("Object"));
                ctx.type = tryToResolveElseRegister(mapType);
            } else {
                var firstItemType = elementExpressionPairs.get(0).expr.type;
                var equalTypes = true;
                Map<String, Type> supportedAccesses = new HashMap<>();
                for (int i = 1, expressionSize = elementExpressionPairs.size(); i < expressionSize; ++i) {
                    var currExprType = elementExpressionPairs.get(i).expr.type;
                    if (!firstItemType.equals(currExprType)) {
                        equalTypes = false;
                    }

                    supportedAccesses.put(elementExpressionPairs.get(i).name.getText(), currExprType);
                }

                if (equalTypes) {
                    ctx.type = tryToResolveElseRegister(new BMLMap(TypeRegistry.resolveType("String"),
                            firstItemType, supportedAccesses));
                } else {
                    ctx.type = tryToResolveElseRegister(new BMLMap(TypeRegistry.resolveType("String"),
                            TypeRegistry.resolveType("Object"), supportedAccesses));
                }
            }
        } else { // List initializer
            // Find type of list items & check they are all equal
            var expressions = ctx.initializer().listInitializer().expression();
            if (expressions.isEmpty()) {
                ctx.type = tryToResolveElseRegister(new BMLList(TypeRegistry.resolveType("Object")));
            } else {
                // Check whether types are homogeneous
                var firstItemType = expressions.get(0).type;
                for (int i = 1, expressionSize = expressions.size(); i < expressionSize; ++i) {
                    if (!firstItemType.equals(expressions.get(i).type)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, "List initialization requires homogeneous types", ctx.initializer());
                        ctx.type = TypeRegistry.resolveType("Object");
                        return;
                    }
                }

                // Types are homogeneous -> try to register type
                ctx.type = tryToResolveElseRegister(new BMLList(firstItemType));
            }
        }
    }

    private Type tryToResolveElseRegister(Type typeToCheck) {
        var resolvedType = TypeRegistry.resolveType(typeToCheck.toString());
        if (resolvedType == null) {
            TypeRegistry.registerType(typeToCheck);
            return typeToCheck;
        } else {
            return resolvedType;
        }
    }

    /*
     * URL Checker
     */
    @Override
    public void exitElementExpressionPair(BMLParser.ElementExpressionPairContext ctx) {
        urlCheck(ctx.expr);
        // TODO: This could be removed once we have implemented parameter checks for annotation, Bot head, etc.
        //       Map initializers could be checked separately
        checkAlreadyDefinedElseDefine(ctx.name);
    }

    private void urlCheck(ParserRuleContext ctx) {
        var url = ctx.getText();
        if (url.equals("url")) {
            url = url.substring(1, url.length() - 1);
            UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"},
                    UrlValidator.ALLOW_LOCAL_URLS + UrlValidator.ALLOW_ALL_SCHEMES);
            if (!urlValidator.isValid(url)) {
                Diagnostics.addDiagnostic(collectedDiagnostics, "Url '%s' is not valid".formatted(url), ctx);
            }
        }
    }
}
