package i5.bml.transpiler.generators;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLList;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

@CodeGenerator(typeClass = BMLList.class)
public class ListGenerator implements Generator {

    @Override
    public Node generateComponent(BMLParser.ComponentContext componentContext, BMLBaseVisitor<Node> visitor) {
        var container = new ClassOrInterfaceDeclaration();

        var args = new NodeList<Expression>();
        if (componentContext.params != null) {
            args.add((Expression) visitor.visit(componentContext.params.elementExpressionPair().get(0).expr));
        } else {
            args.add(new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("ArrayList"), new NodeList<>()));
        }

        BMLList bmlList = (BMLList) componentContext.type;
        var initializer = new MethodCallExpr(new NameExpr("Collections"), "synchronizedList", args);
        FieldDeclaration field = container.addFieldWithInitializer(bmlList.getName(), componentContext.name.getText(),
                initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add import for concurrent hash map
        container.tryAddImportToParentCompilationUnit(List.class);
        container.tryAddImportToParentCompilationUnit(ArrayList.class);

        // Add getter & setter
        container.addMember(field.createGetter());
        container.addMember(field.createSetter());

        return container;
    }

    @Override
    public Node generateFieldAccess(Expression object, TerminalNode field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node generateFunctionCall(BMLParser.FunctionCallContext functionCallContext, BMLBaseVisitor<Node> visitor) {
        throw new UnsupportedOperationException();
    }
}
