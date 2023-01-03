package i5.bml.parser.walker;

import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.symbols.BlockScope;
import i5.bml.parser.types.*;
import i5.bml.parser.types.functions.BMLFunctionScope;
import i5.bml.parser.types.functions.FunctionRegistry;
import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static i5.bml.parser.errors.ParserError.*;

public class DiagnosticsCollector extends BMLBaseListener {

    private final List<Diagnostic> collectedDiagnostics = new ArrayList<>();

    protected Scope currentScope;

    private Scope globalScope;

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
        globalScope = g;
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
    public void enterBotBody(BMLParser.BotBodyContext ctx) {
        for (var bmlFunction : FunctionRegistry.getFunctionsForScope(BMLFunctionScope.GLOBAL)) {
            bmlFunction.defineFunction(currentScope);
        }
    }

    @Override
    public void enterComponent(BMLParser.ComponentContext ctx) {
        checkAlreadyDefinedElseDefine(ctx.name);
    }

    @Override
    public void enterFunctionDefinition(BMLParser.FunctionDefinitionContext ctx) {
        var name = ctx.head.functionName.getText();

        // Check: name already defined in scope
        if (currentScope.resolve(name) != null) {
            Diagnostics.addDiagnostic(collectedDiagnostics, ALREADY_DEFINED.format(name), ctx.head.functionName);
        } else {
            var symbol = new VariableSymbol(name);
            symbol.setType(TypeRegistry.resolveComplexType(BuiltinType.FUNCTION));
            currentScope.define(symbol);
        }

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
        // We aggregate all the allowed accesses from the annotations into the type of the `context` variable
        var contextType = ((AbstractBMLType) TypeRegistry.resolveComplexType(BuiltinType.CONTEXT));
        TypeRegistry.registerType(contextType);

        // Collect supported accesses from annotations
        var supportedAccesses = contextType.getSupportedAccesses();
        ((BMLParser.FunctionDefinitionContext) ctx.parent).annotation().stream()
                .filter(a -> a.type != null)
                .forEach(a -> supportedAccesses.putAll(((AbstractBMLType) a.type).getSupportedAccesses()));

        // Create symbol for context variable (with supported accesses included)
        var contextSymbol = new VariableSymbol("context");
        contextSymbol.setType(contextType);
        currentScope.define(contextSymbol);
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
        else if (ctx.expr != null // We have an expression
                && (ctx.expr.op == null || ctx.expr.op.getType() != BMLParser.DOT) // Expression is not using obj.foo()
                && ctx.expr.functionCall() == null) { // Expression is not a function call
            Diagnostics.addDiagnostic(collectedDiagnostics, NOT_A_STATEMENT.message, ctx.expr);
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

    @Override
    public void enterDialogueAutomaton(BMLParser.DialogueAutomatonContext ctx) {
        checkAlreadyDefinedElseDefine(ctx.head.name);

        // We still want to create a scope, even if name already exists
        Scope s = new BlockScope(currentScope);
        ctx.scope = s;
        pushScope(s);

        for (var bmlFunction : FunctionRegistry.getFunctionsForScope(BMLFunctionScope.DIALOGUE)) {
            bmlFunction.defineFunction(currentScope);
        }
    }

    @Override
    public void exitDialogueAutomaton(BMLParser.DialogueAutomatonContext ctx) {
        var typeName = ctx.head.typeName.getText();
        var dialogueName = ctx.head.name.getText();

        // Check whether type is "allowed"
        if (!TypeRegistry.isTypeBuiltin(typeName)) {
            Diagnostics.addDiagnostic(collectedDiagnostics, UNKNOWN_TYPE.format(typeName), ctx.head.typeName);
            return;
        }

        var resolvedType = typeCheckQualifiedName(typeName, ctx, ctx.head.params);

        // Lastly, we set the type of the corresponding symbol
        var symbol = currentScope.resolve(dialogueName);
        ((VariableSymbol) symbol).setType(resolvedType);

        popScope();
    }

    private void pushScope(Scope s) {
        currentScope = s;
    }

    private void popScope() {
        currentScope = currentScope.getEnclosingScope();
    }

    private void checkAlreadyDefinedElseDefine(Token token) {
        var name = token.getText();

        // Check: name already defined in scope
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

        // Check whether type is "allowed"
        if (!TypeRegistry.isTypeBuiltin(typeName)) {
            Diagnostics.addDiagnostic(collectedDiagnostics, UNKNOWN_TYPE.format(typeName), ctx.typeName);
            return;
        }

        var resolvedType = typeCheckQualifiedName(typeName, ctx, ctx.params);

        // Lastly, we set the type of the corresponding symbol
        var symbol = currentScope.resolve(componentName);
        ((VariableSymbol) symbol).setType(resolvedType);
        ctx.type = resolvedType;
    }

    @Override
    public void exitAnnotation(BMLParser.AnnotationContext ctx) {
        var annotationName = ctx.name.getText();
        var annotationType = TypeRegistry.getBuiltinAnnotation(annotationName);

        if (annotationType == null) {
            Diagnostics.addDiagnostic(collectedDiagnostics, UNKNOWN_ANNOTATION.format(annotationName), ctx.name);
            return;
        }

        ctx.type = typeCheckQualifiedName(annotationType.name() + "Annotation", ctx, ctx.params);
    }

    private Type typeCheckQualifiedName(String typeName, ParserRuleContext ctx, BMLParser.ElementExpressionPairListContext params) {
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
        var diagnosticsCountBefore = collectedDiagnostics.size();
        resolvedType.checkParameters(this, params);
        var diagnosticsCountAfter = collectedDiagnostics.size();

        // 2.2 Populate annotated fields with parameters (required vs optional)
        if (diagnosticsCountBefore == diagnosticsCountAfter) {
            resolvedType.populateParameters(this, params);
        } else {
            resolvedType.populateParameters(this, null);
        }

        var registeredType = (AbstractBMLType) TypeRegistry.resolveType(resolvedType);
        if (registeredType == null) {
            // 3. Invoke initializer (e.g., fetch OpenAPI schemas from provided url parameter)
            // NOTE: We do not pass a diagnosticsCollector to this method since we want the type
            // itself to store its type initialization specific diagnostics. The reason is that
            // this branch is only visited when we encounter a new type (e.g., OpenAPI + new URL)
            resolvedType.initializeType(ctx);
            TypeRegistry.registerType(resolvedType);
        } else {
            // 3. We simply use the already registered type, no need to re-initialize
            resolvedType = registeredType;
        }

        // 4. Add collected diagnostics (either from initialization or cached) with NEW CONTEXT (!)
        // See comment above, this is important for the type caching functionality
        // Whenever we have already initialized type but encountered errors, we want
        // to have them saved such that we do not have to retrieve them again
        var cachedDiagnostics = resolvedType.getCachedDiagnostics();
        for (var diagnostic : cachedDiagnostics) {
            Diagnostics.addDiagnostic(collectedDiagnostics, diagnostic, ctx);
        }

        return resolvedType;
    }

    @Override
    public void enterForEachBody(BMLParser.ForEachBodyContext ctx) {
        var forEachStmtCtx = ((BMLParser.ForEachStatementContext) ctx.parent);
        var exprType = forEachStmtCtx.expr.type;
        Type itemType;
        Type valueType = null;
        if (!(exprType instanceof BMLList) && !(exprType instanceof BMLMap)) {
            Diagnostics.addDiagnostic(collectedDiagnostics, FOREACH_NOT_APPLICABLE.format(exprType), forEachStmtCtx.expr);
            itemType = TypeRegistry.resolveType(BuiltinType.OBJECT);
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
        if (ctx.expr.type instanceof BMLVoid) {
            Diagnostics.addDiagnostic(collectedDiagnostics, "Can't assign expression returning `void`", ctx.name);
            return;
        }

        // When exiting an assignment, we can assume that the right-hand side type was computed already,
        // since it is an expression
        if (ctx.op.getType() == BMLParser.ASSIGN) {
            var name = ctx.name.getText();

            var symbol = globalScope.getSymbol(name);
            if (symbol != null) {
                Diagnostics.addDiagnostic(collectedDiagnostics, "Can't assign a global variable", ctx.name);
                return;
            }

            // NOTE: getSymbol() only checks the current scope. Hence, we might shadow a variable
            //       from an outer scope. This is WANTED. We only locally shadow the variable, since
            //       resolve() starts and the current scope and recursively searches parent scopes
            symbol = currentScope.getSymbol(name);
            if (symbol != null) {
                // We simply redefine the type of the variable, when the variable already exists
                ((VariableSymbol) symbol).setType(ctx.expr.type);
            } else {
                VariableSymbol v = new VariableSymbol(name);
                v.setType(ctx.expr.type);
                currentScope.define(v);
            }
        } else { // Assignment operators with simultaneous arithmetic operation
            // Make sure left-hand side is defined
            var v = currentScope.resolve(ctx.name.getText());
            if (!(v instanceof VariableSymbol)) {
                Diagnostics.addDiagnostic(collectedDiagnostics, NOT_DEFINED.format(ctx.name.getText()), ctx.name);
            } else {
                // Type of left-hand side should already be set
                var leftType = ((VariableSymbol) v).getType();
                var rightType = ctx.expr.type;

                if (ctx.op.getType() == BMLParser.ADD_ASSIGN) {
                    if (!leftType.equals(rightType) || !(leftType instanceof Summable) || !(rightType instanceof Summable)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics,
                                CANNOT_APPLY_OP.format(ctx.op.getText(), ((VariableSymbol) v).getType(), rightType), ctx.name);
                    }
                } else {
                    if (!(leftType instanceof BMLNumber) || !(rightType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics,
                                CANNOT_APPLY_OP.format(ctx.op.getText(), ((VariableSymbol) v).getType(), rightType), ctx.name);
                    }
                }
            }
        }
    }

    @Override
    public void exitAtom(BMLParser.AtomContext ctx) {
        ctx.type = switch (ctx.token.getType()) {
            case BMLParser.IntegerLiteral -> TypeRegistry.resolveType(BuiltinType.NUMBER);
            case BMLParser.FloatingPointLiteral -> TypeRegistry.resolveType(BuiltinType.FLOAT_NUMBER);
            case BMLParser.StringLiteral -> TypeRegistry.resolveType(BuiltinType.STRING);
            case BMLParser.BooleanLiteral -> TypeRegistry.resolveType(BuiltinType.BOOLEAN);
            case BMLParser.Identifier -> {
                var name = ctx.token.getText();
                var resolvedSymbol = currentScope.resolve(name);
                if (!(resolvedSymbol instanceof VariableSymbol)) {
                    Diagnostics.addDiagnostic(collectedDiagnostics, NOT_DEFINED.format(name), ctx.token);
                    // We don't know the type, so we go with Object
                    yield TypeRegistry.resolveType(BuiltinType.OBJECT);
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
                                    CANT_RESOLVE_IN.format(currentCtx.getText(), prevType), ctx.functionCall());
                        }

                        yield TypeRegistry.resolveType(BuiltinType.OBJECT);
                    } else {
                        // In case of a function call, we need to unwrap the BMLFunction type to get the return type
                        if (ctx.functionCall() != null) {
                            if (resolvedType instanceof BMLFunctionType) {
                                // If method: check parameters -> delegate check to BMLFunction
                                resolvedType.checkParameters(this, ctx.functionCall().elementExpressionPairList());
                                ctx.functionCall().type = resolvedType;
                                yield ((BMLFunctionType) resolvedType).getReturnType();
                            } else {
                                yield TypeRegistry.resolveType(BuiltinType.OBJECT);
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
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.LIST, firstExpressionType), firstExpression);
                        yield TypeRegistry.resolveType(BuiltinType.OBJECT);
                    } else if (!(secondExpressionType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER.toString(), secondExpressionType), secondExpression);
                        yield TypeRegistry.resolveType(BuiltinType.OBJECT);
                    } else if (((BMLNumber) secondExpressionType).isFloatingPoint()) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(new BMLNumber(false), secondExpressionType), secondExpression);
                        yield TypeRegistry.resolveType(BuiltinType.OBJECT);
                    } else {
                        // Safe cast because we checked that first expression is a list
                        yield ((BMLList) firstExpressionType).getItemType();
                    }
                }
                case BMLParser.BANG -> {
                    var exprType = ctx.expr.type;

                    if (!(exprType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, exprType), ctx.expr);
                        yield TypeRegistry.resolveType(BuiltinType.BOOLEAN);
                    } else {
                        yield exprType;
                    }
                }
                case BMLParser.LT, BMLParser.LE, BMLParser.GT, BMLParser.GE -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;

                    if (!(leftType instanceof BMLNumber) && !(rightType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, CANNOT_APPLY_OP.format(ctx.op.getText(), leftType, rightType), ctx.left);
                    } else if (!(leftType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, leftType), ctx.left);
                    } else if (!(rightType instanceof BMLNumber)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, rightType), ctx.right);
                    }

                    yield TypeRegistry.resolveType(BuiltinType.BOOLEAN);
                }
                case BMLParser.EQUAL, BMLParser.NOTEQUAL -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;

                    if (!leftType.equals(rightType)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, CANNOT_APPLY_OP.format(ctx.op.getText(), leftType, rightType), ctx);
                    }

                    yield TypeRegistry.resolveType(BuiltinType.BOOLEAN);
                }
                case BMLParser.ADD, BMLParser.SUB, BMLParser.MUL, BMLParser.DIV, BMLParser.MOD -> {
                    if (ctx.left == null) {
                        var expressionType = ctx.expr.type;

                        if (!(expressionType instanceof BMLNumber)) {
                            Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.NUMBER, expressionType), ctx.expr);
                            yield TypeRegistry.resolveType(BuiltinType.NUMBER);
                        } else {
                            yield expressionType;
                        }
                    } else {
                        var leftType = ctx.left.type;
                        var rightType = ctx.right.type;

                        if (ctx.op.getType() == BMLParser.ADD) {
                            if (leftType instanceof Summable && rightType instanceof Summable && leftType.equals(rightType)) {
                                yield leftType;
                            } else if (leftType instanceof BMLNumber && rightType instanceof BMLNumber) {
                                var isLeftOrRightFloat = ((BMLNumber) leftType).isFloatingPoint() || ((BMLNumber) rightType).isFloatingPoint();
                                yield TypeRegistry.resolveType(isLeftOrRightFloat ? BuiltinType.FLOAT_NUMBER : BuiltinType.NUMBER);
                            } else {
                                Diagnostics.addDiagnostic(collectedDiagnostics, CANNOT_APPLY_OP.format("+", leftType, rightType), ctx);
                                yield TypeRegistry.resolveType(BuiltinType.OBJECT);
                            }
                        } else {
                            if (!(leftType instanceof BMLNumber) || !(rightType instanceof BMLNumber)) {
                                Diagnostics.addDiagnostic(collectedDiagnostics,
                                        CANNOT_APPLY_OP.format(ctx.op.getText(), leftType, rightType), ctx.left);
                                yield TypeRegistry.resolveType(BuiltinType.NUMBER);
                            } else {
                                var isLeftOrRightFloat = ((BMLNumber) leftType).isFloatingPoint() || ((BMLNumber) rightType).isFloatingPoint();
                                yield TypeRegistry.resolveType(isLeftOrRightFloat ? BuiltinType.FLOAT_NUMBER : BuiltinType.NUMBER);
                            }
                        }
                    }
                }
                case BMLParser.AND, BMLParser.OR -> {
                    var leftType = ctx.left.type;
                    var rightType = ctx.right.type;

                    if (!(leftType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, leftType), ctx.left);
                        yield TypeRegistry.resolveType(BuiltinType.BOOLEAN);
                    } else if (!(rightType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, rightType), ctx.right);
                        yield TypeRegistry.resolveType(BuiltinType.BOOLEAN);
                    } else {
                        yield leftType;
                    }
                }
                case BMLParser.QUESTION -> {
                    var condType = ctx.expression().get(0).type;
                    var firstType = ctx.expression().get(1).type;
                    var secondType = ctx.expression().get(2).type;

                    if (!(condType instanceof BMLBoolean)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics, EXPECTED_BUT_FOUND.format(BuiltinType.BOOLEAN, condType), ctx.expression().get(0));
                        yield TypeRegistry.resolveType(BuiltinType.OBJECT);
                    }

                    if (!firstType.equals(secondType)) {
                        Diagnostics.addDiagnostic(collectedDiagnostics,
                                "expressions need to have the same type\nFound `%s` : `%s`"
                                        .formatted(firstType, secondType), ctx);
                        yield TypeRegistry.resolveType(BuiltinType.OBJECT);
                    } else {
                        yield firstType;
                    }
                }
                // This should never happen
                default -> throw new IllegalStateException("Unexpected ctx.op: %s\nContext: %s".formatted(ctx.op.getText(), ctx.parent.parent.getText()));
            };
        } else if (ctx.functionCall() != null) {
            ctx.type = ((BMLFunctionType) ctx.functionCall().type).getReturnType();
        } else { // Initializers
            ctx.type = ctx.initializer().type;
        }
    }

    // TODO: Dialogue type checking:
    //       - ERROR: More than one default state
    //       - ERROR: Outgoing state of sink state
    //       - ERROR: RHS of assignment is sink or default
    //       - ERROR:
    //       - WARN:  Unreachable state(s), e.g., state(...)
    //       -


    @Override
    public void exitFunctionCall(BMLParser.FunctionCallContext ctx) {
        // We make sure that we are not checking a function call on an object, e.g., api.get(...)
        // This is handled by the expression type checking
        if (ctx.parent instanceof BMLParser.ExpressionContext expressionContext
                && expressionContext.op != null
                && expressionContext.op.getType() == BMLParser.DOT) {
            return;
        }

        var name = ctx.functionName.getText();
        var symbol = currentScope.resolve(name);
        if (symbol == null) {
            Diagnostics.addDiagnostic(collectedDiagnostics, NOT_DEFINED.format(name), ctx.functionName);
            ctx.type = TypeRegistry.resolveType(BuiltinType.OBJECT);
            ctx.type = TypeRegistry.resolveType(BuiltinType.OBJECT);
            return;
        }

        // Perform type checks for function calls
        var functionType = new BMLFunctionType(((BMLFunctionType) ((TypedSymbol) symbol).getType()));
        functionType.checkParameters(this, ctx.params);
        functionType.initializeType(ctx);
        ctx.type = functionType;
    }

    @Override
    public void exitDialogueTransition(BMLParser.DialogueTransitionContext ctx) {
        for (var functionCallContext : ctx.functionCall()) {
            var name = functionCallContext.functionName.getText();
            var symbol = currentScope.resolve(name);
            if (symbol == null) {
                Diagnostics.addDiagnostic(collectedDiagnostics, NOT_DEFINED.format(name), functionCallContext.functionName);
                functionCallContext.type = TypeRegistry.resolveType(BuiltinType.OBJECT);
                return;
            }

            // Perform type checks for function calls
            var functionType = new BMLFunctionType(((BMLFunctionType) ((TypedSymbol) symbol).getType()));
            functionType.checkParameters(this, functionCallContext.params);
            functionType.initializeType(functionCallContext);

            functionCallContext.type = functionType;
        }
    }

    @Override
    public void exitInitializer(BMLParser.InitializerContext ctx) {
        if (ctx.mapInitializer() != null) {
            ctx.type = ctx.mapInitializer().type;
        } else { // list initializer
            ctx.type = ctx.listInitializer().type;
        }
    }

    @Override
    public void exitMapInitializer(BMLParser.MapInitializerContext ctx) {
        var params = ctx.params;
        if (params == null) {
            var map = new BMLMap();
            map.initializeType(null);
            ctx.type = tryToResolveElseRegister(map);
        } else {
            var elementExpressionPairs = params.elementExpressionPair();
            Map<String, Type> supportedAccesses = new HashMap<>();

            Type firstType = null;
            if (elementExpressionPairs.size() > 0) {
                firstType = elementExpressionPairs.get(0).expr.type;
            }

            var sameValueType = true;

            for (var elementExpressionPair : elementExpressionPairs) {
                if (!elementExpressionPair.expr.type.equals(firstType)) {
                    sameValueType = false;
                }

                supportedAccesses.put(elementExpressionPair.name.getText(), elementExpressionPair.expr.type);
            }

            var valueType = sameValueType ? firstType : TypeRegistry.resolveType(BuiltinType.OBJECT);
            var map = new BMLMap(TypeRegistry.resolveType(BuiltinType.STRING), valueType, supportedAccesses);
            map.initializeType(null);
            ctx.type = tryToResolveElseRegister(map);
        }
    }

    @Override
    public void exitListInitializer(BMLParser.ListInitializerContext ctx) {
        // Find type of list items & check they are all equal
        var expressions = ctx.expression();
        if (expressions.isEmpty()) {
            ctx.type = tryToResolveElseRegister(new BMLList(TypeRegistry.resolveType(BuiltinType.OBJECT)));
        } else {
            // Check whether types are homogeneous
            var firstItemType = expressions.get(0).type;
            for (int i = 1, expressionSize = expressions.size(); i < expressionSize; ++i) {
                if (!firstItemType.equals(expressions.get(i).type)) {
                    Diagnostics.addDiagnostic(collectedDiagnostics, LIST_BAD_TYPES.message, ctx);
                    ctx.type = TypeRegistry.resolveType(BuiltinType.OBJECT);
                    return;
                }
            }

            // Types are homogeneous -> try to register type
            ctx.type = tryToResolveElseRegister(new BMLList(firstItemType));
        }
    }

    private Type tryToResolveElseRegister(Type typeToCheck) {
        var resolvedType = TypeRegistry.resolveType(typeToCheck);
        if (resolvedType == null) {
            TypeRegistry.registerType(typeToCheck);
            return typeToCheck;
        } else {
            return resolvedType;
        }
    }
}
