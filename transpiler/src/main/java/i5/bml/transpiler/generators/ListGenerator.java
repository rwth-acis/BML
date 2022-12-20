package i5.bml.transpiler.generators;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
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
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

@CodeGenerator(typeClass = BMLList.class)
public class ListGenerator implements Generator {

    public ListGenerator(Type listType) {
    }

    @Override
    public Node generateComponent(BMLParser.ComponentContext ctx, BMLBaseVisitor<Node> visitor) {
        var compilationUnit = new CompilationUnit();
        var container = compilationUnit.addClass("Container");

        var args = new NodeList<Expression>();
        if (ctx.params != null) {
            args.add((Expression) visitor.visit(ctx.params.elementExpressionPair().get(0).expr));
        } else {
            args.add(new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("ArrayList"), new NodeList<>()));
        }

        BMLList bmlList = (BMLList) ctx.type;
        var initializer = new MethodCallExpr(new NameExpr("Collections"), "synchronizedList", args);
        FieldDeclaration field = container.addFieldWithInitializer(bmlList.getName(), ctx.name.getText(),
                initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add import for concurrent hash map
        compilationUnit.addImport(List.class);
        compilationUnit.addImport(ArrayList.class);

        // Add getter & setter
        field.createGetter();
        field.createSetter();

        return compilationUnit;
    }
}
