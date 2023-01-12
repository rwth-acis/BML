package i5.bml.transpiler.generators.types.components.primitives;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.primitives.BMLNumber;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import org.antlr.symtab.Type;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

@CodeGenerator(typeClass = BMLNumber.class)
public class NumberGenerator extends Generator {

    public NumberGenerator(Type numberType) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        var type = StaticJavaParser.parseClassOrInterfaceType("AtomicLong");
        var initializer = new ObjectCreationExpr(null, type, new NodeList<>());
        FieldDeclaration field = currentClass.addFieldWithInitializer(type, ctx.name.getText(),
                initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add import
        //noinspection OptionalGetWithoutIsPresent -> We can assume the presence
        currentClass.findCompilationUnit().get().addImport(AtomicLong.class);

        // Add getter & setter
        var getter = field.createGetter();
        getter.addModifier(Modifier.Keyword.STATIC);
    }

    @Override
    public Node generateArithmeticAssignmentToGlobal(BMLParser.AssignmentContext ctx, BinaryExpr.Operator op, JavaTreeGenerator visitor) {
        var expr = (Expression) visitor.visit(ctx.expr);
        BlockStmt block = new BlockStmt();
        expr.stream().filter(node -> node instanceof NameExpr).forEach(n -> {
            var oldName = ((NameExpr) n).getName();
            var newName = oldName + "Copy";
            var newVarDecl = new VariableDeclarationExpr(new VariableDeclarator(new VarType(), newName, new NameExpr(oldName)));
            newVarDecl.setFinal(true);
            block.addStatement(newVarDecl);
            ((NameExpr) n).setName(newName);
        });

        if (expr instanceof BinaryExpr) {
            expr = new EnclosedExpr(expr);
        }

        var lambdaBodyExpr = new BinaryExpr(new NameExpr("number"), expr, op);
        var lambdaExpr = new LambdaExpr(new Parameter(new UnknownType(), "number"), lambdaBodyExpr);
        var m = "ComponentRegistry.get%s().updateAndGet".formatted(StringUtils.capitalize(ctx.name.getText()));
        block.addStatement(new MethodCallExpr(m, lambdaExpr));

        return block;
    }

    @Override
    public Node generateAddAssignment(BMLParser.AssignmentContext ctx, JavaTreeGenerator visitor) {
        return new AssignExpr(new NameExpr(ctx.name.getText()), (Expression) visitor.visit(ctx.expr), AssignExpr.Operator.PLUS);
    }
}
