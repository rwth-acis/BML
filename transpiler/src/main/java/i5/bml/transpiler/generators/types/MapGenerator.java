package i5.bml.transpiler.generators.types;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.PrimitiveType;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.*;
import i5.bml.transpiler.BMLTypeResolver;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.GeneratorRegistry;
import i5.bml.transpiler.generators.HasBotClass;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CodeGenerator(typeClass = BMLMap.class)
public class MapGenerator implements Generator {

    public MapGenerator(Type mapType) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaSynthesizer visitor) {
        var currentClass = visitor.getCurrentClass();
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
        addImportForClass(bmlMapType.getKeyType(), compilationUnit, visitor.getOutputPackage());
        addImportForClass(bmlMapType.getValueType(), compilationUnit, visitor.getOutputPackage());

        // Add getter & setter
        var getter = field.createGetter();
        getter.addModifier(Modifier.Keyword.STATIC);
    }

    private void addImportForClass(Type type, CompilationUnit compilationUnit, String outputPackage) {
        var generator = GeneratorRegistry.getGeneratorForType(type);
        if (generator instanceof HasBotClass botClassGenerator) {
            compilationUnit.addImport(Utils.renameImport(botClassGenerator.getBotClass(), outputPackage), false, false);
        }
    }

    @Override
    public Node generateInitializer(ParserRuleContext ctx, JavaSynthesizer visitor) {
        var params = ((BMLParser.MapInitializerContext) ctx).params;
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        visitor.getCurrentClass().findCompilationUnit().get().addImport(Map.class);
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
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaSynthesizer visitor) {
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
