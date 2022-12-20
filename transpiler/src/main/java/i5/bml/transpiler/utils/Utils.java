package i5.bml.transpiler.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

public class Utils {

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

    public static ClassOrInterfaceDeclaration readClass(String path, String className) {
        var javaFilePath = "%s/%s.java".formatted(path, className);
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(new File(javaFilePath));
            //noinspection OptionalGetWithoutIsPresent -> We can assume that the class is present
            return compilationUnit.getClassByName(className).get();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(javaFilePath));
        }
    }
}
