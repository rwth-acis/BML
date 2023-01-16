package i5.bml.transpiler.utils;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.Printer;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import i5.bml.transpiler.generators.java.JavaTreeVisitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.function.Consumer;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "ResultOfMethodCallIgnored"})
public class PrinterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterUtil.class);

    private static final Printer PRINTER = new DefaultPrettyPrinter(JavaTreeVisitor::new, new DefaultPrinterConfiguration());

    private PrinterUtil() {}

    static {
        StaticJavaParser.setConfiguration(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
    }

    public static void writeClass(String path, String fileName, CompilationUnit compilationUnit) {
        var filePath = "%s/%s.java".formatted(path, fileName);
        var javaFile = new File(filePath);
        try {
            javaFile.getParentFile().mkdirs();
            javaFile.createNewFile();

            var fileOutputStream = new FileOutputStream(javaFile);

            sortClassMembersAndImports(compilationUnit.getClassByName(fileName).get());
            fileOutputStream.getChannel().write(ByteBuffer.wrap(PRINTER.print(compilationUnit).getBytes()));

            fileOutputStream.close();
        } catch (IOException e) {
            LOGGER.error("Error writing class {}", filePath, ExceptionUtils.getRootCause(e));
        }
    }

    public static void readAndWriteClass(String path, String fileName, String className, Consumer<ClassOrInterfaceDeclaration> c) {
        var javaFilePath = "%s/%s.java".formatted(path, fileName);
        var javaFile = new File(javaFilePath);

        CompilationUnit compilationUnit;
        try {
            compilationUnit = StaticJavaParser.parse(javaFile);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error reading class {}", javaFilePath, ExceptionUtils.getRootCause(e));
            return;
        }

        try (var fileOutputStream = new FileOutputStream(javaFile)) {
            var clazz = compilationUnit.getClassByName(className).get();
            c.accept(clazz);
            sortClassMembersAndImports(clazz);
            fileOutputStream.getChannel().write(ByteBuffer.wrap(PRINTER.print(compilationUnit).getBytes()));
        } catch (IOException e) {
            LOGGER.error("Error writing class {}", javaFilePath, ExceptionUtils.getRootCause(e));
        }
    }

    private static void sortClassMembersAndImports(ClassOrInterfaceDeclaration clazz) {
        clazz.getMembers().sort(Comparator.comparing((BodyDeclaration<?> t) -> t.isMethodDeclaration())
                .thenComparing(BodyDeclaration::isConstructorDeclaration)
                .thenComparing(BodyDeclaration::isFieldDeclaration));

        clazz.findCompilationUnit().get().getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));
    }

    public static void readAndWriteClass(String botOutputPath, Class<?> clazz, Consumer<ClassOrInterfaceDeclaration> c) {
        readAndWriteClass(botOutputPath, clazz.getSimpleName(), clazz, c);
    }

    public static void readAndWriteClass(String botOutputPath, String fileName, Class<?> clazz, Consumer<ClassOrInterfaceDeclaration> c) {
        var packageName = clazz.getPackageName()
                .replace("i5.bml.transpiler.bot", "")
                .replaceFirst("\\.", "")
                .replaceAll("\\.", "/");
        readAndWriteClass(botOutputPath + packageName, fileName, clazz.getSimpleName(), c);
    }

    public static ClassOrInterfaceDeclaration readClass(String path, String className) {
        var javaFilePath = "%s/%s.java".formatted(path, className);
        try {
            var javaFile = new File(javaFilePath);
            CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
            return compilationUnit.getClassByName(className).get();
        } catch (FileNotFoundException e) {
            LOGGER.error("Error reading class {}", javaFilePath, ExceptionUtils.getRootCause(e));
            return new ClassOrInterfaceDeclaration();
        }
    }

    public static ClassOrInterfaceDeclaration readClass(String botOutputPath, Class<?> clazz) {
        var packageName = clazz.getPackageName()
                .replace("i5.bml.transpiler.bot", "")
                .replaceFirst("\\.", "")
                .replaceAll("\\.", "/");
        return readClass(botOutputPath + packageName, clazz.getSimpleName());
    }

    public static void writeClass(String path, Class<?> clazz, ClassOrInterfaceDeclaration classToWrite) {
        var packageName = clazz.getPackageName()
                .replace("i5.bml.transpiler.bot", "")
                .replaceFirst("\\.", "")
                .replaceAll("\\.", "/");
        writeClass(path + packageName, classToWrite.findCompilationUnit().get(), classToWrite);
    }

    public static void writeClass(String path, CompilationUnit compilationUnit, ClassOrInterfaceDeclaration clazz) {
        var javaFilePath = "%s/%s.java".formatted(path, clazz.getName());
        var javaFile = new File(javaFilePath);
        try (var fileOutputStream = new FileOutputStream(javaFile)) {
            sortClassMembersAndImports(clazz);
            fileOutputStream.getChannel().write(ByteBuffer.wrap(PRINTER.print(compilationUnit).getBytes()));
        } catch (IOException e) {
            LOGGER.error("Error writing class {}", javaFilePath, ExceptionUtils.getRootCause(e));
        }
    }

    public static void copyClass(String path, String oldName, String newName) {
        try {
            FileUtils.copyFile(new File("%s/%s.java".formatted(path, oldName)), new File("%s/%s.java".formatted(path, newName)));
            readAndWriteClass(path, newName, oldName, clazz -> {
                clazz.setName(newName);
                clazz.getConstructors().forEach(c -> c.setName(newName));
            });
        } catch (IOException e) {
            LOGGER.error("Error copying class {} at {}", oldName, path, ExceptionUtils.getRootCause(e));
        }
    }
}
