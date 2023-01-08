package i5.bml.transpiler.utils;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.antlr.v4.runtime.RuleContext;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class Utils {

    public static boolean isJavaHomeDefined() {
        String javaHome = System.getenv("JAVA_HOME");
        return javaHome != null;
    }

    public static String pascalCaseToSnakeCase(String input) {
        char[] charArray = input.toCharArray();
        StringBuilder out = new StringBuilder("" + charArray[0]);
        for (int i = 1, charArrayLength = charArray.length; i < charArrayLength; i++) {
            char c = charArray[i];
            if (Character.isUpperCase(c)) {
                out.append("_").append(c);
            } else {
                out.append(Character.toUpperCase(c));
            }
        }

        return out.toString();
    }

    public static String renameImport(Class<?> clazz, String outputPackage) {
        var packageName = clazz.getName().replaceFirst("i5.bml.transpiler.bot.", "");
        if (!outputPackage.isEmpty()) {
            packageName = "%s.%s".formatted(outputPackage, packageName);
        }
        return packageName;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void generateToStringMethod(ClassOrInterfaceDeclaration clazz) {
        var className = clazz.getNameAsString();
        StringBuilder stringBuilder = new StringBuilder(StringUtils.capitalize(className));
        stringBuilder.append("{");

        var fields = clazz.getFields();
        var args = new NodeList<Expression>();
        for (var field : fields) {
            var name = field.getVariable(0).getName();
            stringBuilder.append(name).append("=%s, ");
            args.add(new NameExpr(name));
        }

        stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "}");

        var toStringMethod = clazz.addMethod("toString", Modifier.Keyword.PUBLIC);
        toStringMethod.setType(String.class);
        toStringMethod.getBody().get().addStatement(new ReturnStmt(new MethodCallExpr(new StringLiteralExpr(stringBuilder.toString()), "formatted", args)));
    }

    public static void generateRecordStyleGetter(FieldDeclaration field, boolean makeStatic) {
        var getter = field.createGetter();
        // Remove "get" prefix
        getter.setName(StringUtils.uncapitalize(getter.getNameAsString().substring(3)));

        if (makeStatic) {
            getter.addModifier(Modifier.Keyword.STATIC);
        }
    }

    public static <T> T findParentContext(RuleContext currentCtx, Class<T> parentCtxClass) {
        var currParent = currentCtx;
        while (currParent != null && !parentCtxClass.isInstance(currParent)) {
            currParent = currParent.parent;
        }

        //noinspection unchecked
        return currParent != null ? (T) currParent : null;
    }
}
