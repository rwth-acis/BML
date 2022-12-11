package i5.bml.transpiler.generators;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLMap;
import i5.bml.transpiler.BMLTypeResolver;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.concurrent.ConcurrentHashMap;

@CodeGenerator(typeClass = BMLMap.class)
public class MapGenerator implements Generator {

    @Override
    public Node generateComponent(BMLParser.ComponentContext componentContext, BMLBaseVisitor<Node> visitor) {
        var container = new ClassOrInterfaceDeclaration();

        // Add component itself
        var args = new NodeList<Expression>();
        if (componentContext.params != null) {
            args.add((Expression) visitor.visit(componentContext.params.elementExpressionPair().get(0).expr));
        }

        var initializer = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("ConcurrentHashMap"), args);
        BMLMap bmlMapType = (BMLMap) componentContext.type;
        var keyType = BMLTypeResolver.resolveBMLTypeToJavaType(bmlMapType.getKeyType());
        var valueType = BMLTypeResolver.resolveBMLTypeToJavaType(bmlMapType.getValueType());
        var javaMapType = "Map<%s, %s>".formatted(keyType, valueType);
        FieldDeclaration field = container.addFieldWithInitializer(javaMapType, componentContext.name.getText(), initializer,
                Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);

        // Add import for concurrent hash map
        container.tryAddImportToParentCompilationUnit(ConcurrentHashMap.class);

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
