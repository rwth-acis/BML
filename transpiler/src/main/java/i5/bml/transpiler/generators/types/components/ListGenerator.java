package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLList;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CodeGenerator(typeClass = BMLList.class)
public class ListGenerator implements Generator {

    public ListGenerator(Type listType) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaSynthesizer visitor) {
        var currentClass = visitor.getCurrentClass();
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
    public Node generateInitializer(ParserRuleContext ctx, JavaSynthesizer visitor) {
        var arguments = ((BMLParser.ListInitializerContext) ctx).expression().stream()
                .map(e -> (Expression) visitor.visit(e))
                .collect(Collectors.toCollection(NodeList::new));
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var compilationUnit = visitor.getCurrentClass().findCompilationUnit().get();
        compilationUnit.addImport(List.class);
        return new MethodCallExpr(new NameExpr("List"), new SimpleName("of"), arguments);
    }
}
