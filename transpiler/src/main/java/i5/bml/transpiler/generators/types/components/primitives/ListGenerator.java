package i5.bml.transpiler.generators.types.components.primitives;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.primitives.BMLList;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.GeneratorRegistry;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CodeGenerator(typeClass = BMLList.class)
public class ListGenerator extends Generator {

    public ListGenerator(Type listType) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();
        //noinspection OptionalGetWithoutIsPresent -> We can assume the presence
        var compilationUnit = currentClass.findCompilationUnit().get();

        var args = new NodeList<Expression>();
        if (ctx.params != null) {
            args.add((Expression) visitor.visit(ctx.params.elementExpressionPair().get(0).expr));
        } else {
            args.add(new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("ArrayList"), new NodeList<>()));
        }

        BMLList bmlList = (BMLList) ctx.type;
        var initializer = new MethodCallExpr(new NameExpr("Collections"), "synchronizedList", args);
        FieldDeclaration field = currentClass.addFieldWithInitializer(bmlList.getName(), ctx.name.getText(),
                initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add import for list types
        compilationUnit.addImport(List.class);
        compilationUnit.addImport(ArrayList.class);

        // Add getter & setter
        var getter = field.createGetter();
        getter.setModifiers(Modifier.Keyword.STATIC);
        var setter = field.createSetter();
        setter.setModifiers(Modifier.Keyword.STATIC);
    }

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        return switch (ctx.functionName.getText()) {
            case "join" -> {
                var delimiter = ((BMLFunctionType) ctx.type).getRequiredParameters().get(0).getExprCtx();
                yield new MethodCallExpr(new NameExpr("String"), "join", new NodeList<>((Expression) visitor.visit(delimiter), object));
            }
            default -> null;
        };
    }

    @Override
    public Node generateInitializer(ParserRuleContext ctx, JavaTreeGenerator visitor) {
        var arguments = ((BMLParser.ListInitializerContext) ctx).expression().stream()
                .map(e -> (Expression) visitor.visit(e))
                .collect(Collectors.toCollection(NodeList::new));
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var compilationUnit = visitor.currentClass().findCompilationUnit().get();
        compilationUnit.addImport(List.class);
        return new MethodCallExpr(new NameExpr("List"), new SimpleName("of"), arguments);
    }

    @Override
    public Node generateFieldAccess(Expression object, TerminalNode field) {
        if (field.getText().equals("code")) {
            return new NameExpr(object.asNameExpr() + "Code");
        } else {
            return new MethodCallExpr(object, new SimpleName("get" + StringUtils.capitalize(field.getText())));
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public com.github.javaparser.ast.type.Type generateVariableType(Type type, JavaTreeGenerator visitor) {
        var cu = visitor.currentClass().findCompilationUnit().get();
        cu.addImport(List.class);
        var itemType = ((BMLList) type).getItemType();
        var javaItemType = GeneratorRegistry.generatorForType(itemType).generateVariableType(itemType, visitor);
        return new ClassOrInterfaceType(null, "List").setTypeArguments(new NodeList<>(javaItemType));
    }
}
