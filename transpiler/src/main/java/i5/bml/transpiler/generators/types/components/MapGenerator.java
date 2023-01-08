package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.components.BMLMap;
import i5.bml.transpiler.generators.*;
import i5.bml.transpiler.generators.types.BMLTypeResolver;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CodeGenerator(typeClass = BMLMap.class)
public class MapGenerator extends Generator {

    public MapGenerator(Type mapType) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();
        //noinspection OptionalGetWithoutIsPresent -> We can assume the presence
        var compilationUnit = currentClass.findCompilationUnit().get();

        // Add component itself
        var args = new NodeList<Expression>();
        if (ctx.params != null) {
            args.add((Expression) visitor.visit(ctx.params.elementExpressionPair().get(0).expr));
        }

        var initializer = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("ConcurrentHashMap<>"), args);
        BMLMap bmlMapType = (BMLMap) ctx.type;

        var keyType = BMLTypeResolver.resolveBMLTypeToJavaType(bmlMapType.getKeyType());
        var valueType = BMLTypeResolver.resolveBMLTypeToJavaType(bmlMapType.getValueType());
        var javaMapType = "Map<%s, %s>".formatted(StringUtils.capitalize(keyType.asString()),
                StringUtils.capitalize(valueType.asString()));
        FieldDeclaration field = currentClass.addFieldWithInitializer(javaMapType, ctx.name.getText(), initializer,
                Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);

        // Add import for concurrent hash map
        compilationUnit.addImport(Map.class);
        compilationUnit.addImport(ConcurrentHashMap.class);

        // Add import for key and value types
        addImportForClass(bmlMapType.getKeyType(), compilationUnit, visitor.outputPackage());
        addImportForClass(bmlMapType.getValueType(), compilationUnit, visitor.outputPackage());

        // Add getter & setter
        var getter = field.createGetter();
        getter.addModifier(Modifier.Keyword.STATIC);
    }

    private void addImportForClass(Type type, CompilationUnit compilationUnit, String outputPackage) {
        var generator = GeneratorRegistry.generatorForType(type);
        if (generator instanceof HasBotClass botClassGenerator) {
            compilationUnit.addImport(Utils.renameImport(botClassGenerator.getBotClass(), outputPackage), false, false);
        }
    }

    @Override
    public Node generateInitializer(ParserRuleContext ctx, JavaTreeGenerator visitor) {
        var params = ((BMLParser.MapInitializerContext) ctx).params;
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        visitor.currentClass().findCompilationUnit().get().addImport(Map.class);
        if (params != null) {
            var arguments = params.elementExpressionPair().stream()
                    .flatMap(p -> Stream.of(new StringLiteralExpr(p.name.getText()), (Expression) visitor.visit(p.expr)))
                    .collect(Collectors.toCollection(NodeList::new));
            return new MethodCallExpr(new NameExpr("Map"), "of", arguments);
        } else {
            return new MethodCallExpr(new NameExpr("Map"), "of");
        }
    }

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var params = new ArrayList<>(((BMLFunctionType) ctx.type).getRequiredParameters());
        var args = params.stream().map(p -> (Expression) visitor.visit(p.getExprCtx())).collect(Collectors.toCollection(NodeList::new));
        var name = ctx.functionName.getText().equals("add") ? "put" : ctx.functionName.getText();
        return new MethodCallExpr(object, name, args);
    }

    @Override
    public Node generateNameExpr(BMLParser.AtomContext ctx) {
        return new MethodCallExpr("ComponentRegistry.get%s".formatted(StringUtils.capitalize(ctx.token.getText())));
    }
}
