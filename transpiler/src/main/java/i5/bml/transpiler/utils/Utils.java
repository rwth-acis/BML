package i5.bml.transpiler.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import i5.bml.transpiler.bot.components.ComponentInitializer;
import i5.bml.transpiler.bot.threads.telegram.TelegramBotThread;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

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

    public static ReturnStmt generateToStringMethod(String className, List<FieldDeclaration> fields) {
        StringBuilder stringBuilder = new StringBuilder(StringUtils.capitalize(className));
        stringBuilder.append("{");

        var args = new NodeList<Expression>();
        for (var field : fields) {
            var name = field.getVariable(0).getName();
            stringBuilder.append(name).append("=%s, ");
            args.add(new NameExpr(name));
        }

        stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "}");

        return new ReturnStmt(new MethodCallExpr(new StringLiteralExpr(stringBuilder.toString()), "formatted", args));
    }

    public static void generateRecordStyleGetter(FieldDeclaration field) {
        var getter = field.createGetter();
        // Remove "get" prefix
        getter.setName(StringUtils.uncapitalize(getter.getNameAsString().substring(3)));
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
