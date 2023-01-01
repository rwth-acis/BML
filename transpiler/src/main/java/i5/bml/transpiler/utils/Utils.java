package i5.bml.transpiler.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.function.Consumer;

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

    public static void writeClass(String path, String fileName, CompilationUnit compilationUnit) {
        var filePath = "%s/%s.java".formatted(path, fileName);
        try {
            var javaFile = new File(filePath);
            var javaFilePath = javaFile.toPath();
            Files.createDirectories(javaFilePath.getParent());
            Files.createFile(javaFilePath);
            Files.write(javaFilePath, compilationUnit.toString().getBytes());
        } catch (NoSuchFileException | FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(filePath));
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s".formatted(filePath), e);
        }
    }

    public static void readAndWriteJavaFile(File file, String className, Consumer<TypeDeclaration<?>> c) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);

            if (compilationUnit.getClassByName(className).isPresent()) {
                c.accept(compilationUnit.getClassByName(className).get());
            } else if (compilationUnit.getEnumByName(className).isPresent()) {
                c.accept(compilationUnit.getEnumByName(className).get());
            } else if (compilationUnit.getInterfaceByName(className).isPresent()) {
                c.accept(compilationUnit.getInterfaceByName(className).get());
            } else if (compilationUnit.getAnnotationDeclarationByName(className).isPresent()) {
                c.accept(compilationUnit.getAnnotationDeclarationByName(className).get());
            } else {
                throw new IllegalStateException("%s is neither a class, enum, nor interface".formatted(className));
            }

            Files.write(file.toPath(), compilationUnit.toString().getBytes());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(file.getAbsolutePath()));
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s: %s".formatted(file.getAbsolutePath(), e.getMessage()));
        }
    }

    public static void readAndWriteClass(String path, String fileName, String className, Consumer<ClassOrInterfaceDeclaration> c) {
        var javaFilePath = "%s/%s.java".formatted(path, fileName);
        try {
            var javaFile = new File(javaFilePath);
            CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);

            //noinspection OptionalGetWithoutIsPresent -> We can assume that the class is present
            c.accept(compilationUnit.getClassByName(className).get());

            Files.write(javaFile.toPath(), compilationUnit.toString().getBytes());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(javaFilePath));
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s: %s".formatted(javaFilePath, e.getMessage()));
        }
    }

    public static void readAndWriteClass(String path, String className, Consumer<ClassOrInterfaceDeclaration> c) {
        readAndWriteClass(path, className, className, c);
    }

    public static void readAndWriteClass(String botOutputPath, Class<?> clazz, Consumer<ClassOrInterfaceDeclaration> c) {
        var packageName = clazz.getPackageName().replace("i5.bml.transpiler.bot.", "").replace(".", "");
        readAndWriteClass(botOutputPath + packageName, clazz.getSimpleName(), clazz.getSimpleName(), c);
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
