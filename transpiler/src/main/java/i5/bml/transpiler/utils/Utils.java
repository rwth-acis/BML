package i5.bml.transpiler.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.Printer;
import com.github.javaparser.printer.SourcePrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import i5.bml.transpiler.generators.JavaTreeVisitor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
}
