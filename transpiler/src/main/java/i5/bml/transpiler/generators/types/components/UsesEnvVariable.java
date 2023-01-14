package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

public interface UsesEnvVariable {

    default Expression getFromEnv(String parameter) {
        if (parameter.startsWith("env:")) {
            return new MethodCallExpr(new NameExpr("System"), "getenv", new NodeList<>(new StringLiteralExpr(parameter.substring(4))));
        } else {
            return new StringLiteralExpr(parameter);
        }
    }
}
