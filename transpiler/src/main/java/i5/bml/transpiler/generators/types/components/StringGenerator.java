package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.BMLString;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;
import org.apache.commons.lang3.StringUtils;

@CodeGenerator(typeClass = BMLString.class)
public class StringGenerator extends Generator {

    public StringGenerator(Type stringType) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        var type = StaticJavaParser.parseClassOrInterfaceType("StringBuffer");
        var initializer = new ObjectCreationExpr(null, type, new NodeList<>());
        FieldDeclaration field = currentClass.addFieldWithInitializer(type, ctx.name.getText(),
                initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add import
        //noinspection OptionalGetWithoutIsPresent -> We can assume the presence
        currentClass.findCompilationUnit().get().addImport(StringBuffer.class);

        // Add getter & setter
        var getter = field.createGetter();
        getter.addModifier(Modifier.Keyword.STATIC);
    }

    @Override
    public Node generateArithmeticAssignmentToGlobal(BMLParser.AssignmentContext ctx, BinaryExpr.Operator op, JavaTreeGenerator visitor) {
        var m = "ComponentRegistry.get%s().append".formatted(StringUtils.capitalize(ctx.name.getText()));
        return new MethodCallExpr(m, (Expression) visitor.visit(ctx.expr));
    }

    @Override
    public Node generateAddAssignment(BMLParser.AssignmentContext ctx, JavaTreeGenerator visitor) {
        return new AssignExpr(new NameExpr(ctx.name.getText()), (Expression) visitor.visit(ctx.expr), AssignExpr.Operator.PLUS);
    }
}
