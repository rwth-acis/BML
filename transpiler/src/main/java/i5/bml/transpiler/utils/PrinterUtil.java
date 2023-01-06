package i5.bml.transpiler.utils;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.Printer;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import i5.bml.transpiler.generators.JavaTreeVisitor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;

public class PrinterUtil {

    private static final PrinterConfiguration configuration = new DefaultPrinterConfiguration();

    private static final Function<PrinterConfiguration, VoidVisitor<Void>> visitorFactory = JavaTreeVisitor::new;

    private static final Printer printer = new DefaultPrettyPrinter(visitorFactory, configuration);

    static {
        StaticJavaParser.setConfiguration(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
    }

    public static void writeClass(String path, String fileName, CompilationUnit compilationUnit) {
        var filePath = "%s/%s.java".formatted(path, fileName);
        try {
            var javaFile = new File(filePath);
            var javaFilePath = javaFile.toPath();
            Files.createDirectories(javaFilePath.getParent());
            Files.createFile(javaFilePath);
            //noinspection OptionalGetWithoutIsPresent -> We can assume presence
            sortClassMembersAndImports(compilationUnit.getClassByName(fileName).get());
            Files.write(javaFilePath, printer.print(compilationUnit).getBytes());
        } catch (NoSuchFileException | FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(filePath), e);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s".formatted(filePath), e);
        }
    }

    public static void readAndWriteJavaFile(File file, String className, Consumer<TypeDeclaration<?>> c) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);

            if (compilationUnit.getPrimaryType().isPresent()) {
                c.accept(compilationUnit.getPrimaryType().get());
            } else {
                throw new IllegalStateException("%s doesn't seem to have a primary type declaration (i.e., class, enum, etc.)".formatted(className));
            }

            Files.write(file.toPath(), printer.print(compilationUnit).getBytes());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(file.getAbsolutePath()), e);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s: %s".formatted(file.getAbsolutePath(), e.getMessage()), e);
        }
    }

    public static void readAndWriteClass(String path, String fileName, String className, Consumer<ClassOrInterfaceDeclaration> c) {
        var javaFilePath = "%s/%s.java".formatted(path, fileName);
        try {
            var javaFile = new File(javaFilePath);
            CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
            //noinspection OptionalGetWithoutIsPresent -> We can assume that the class is present
            var clazz = compilationUnit.getClassByName(className).get();
            c.accept(clazz);
            sortClassMembersAndImports(clazz);
            Files.write(javaFile.toPath(), printer.print(compilationUnit).getBytes());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(javaFilePath), e);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s: %s".formatted(javaFilePath, e.getMessage()), e);
        }
    }

    public static void copyClass(String path, String fileName, String className, Consumer<ClassOrInterfaceDeclaration> c) {

    }

    private static void sortClassMembersAndImports(ClassOrInterfaceDeclaration clazz) {
        clazz.getMembers().sort(Comparator.comparing((BodyDeclaration<?> t) -> t.isMethodDeclaration())
                .thenComparing(BodyDeclaration::isConstructorDeclaration)
                .thenComparing(BodyDeclaration::isFieldDeclaration));

        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        clazz.findCompilationUnit().get().getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));
    }

    public static void readAndWriteClass(String path, String className, Consumer<ClassOrInterfaceDeclaration> c) {
        readAndWriteClass(path, className, className, c);
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
            //noinspection OptionalGetWithoutIsPresent -> We can assume that the class is present
            return compilationUnit.getClassByName(className).get();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find %s".formatted(javaFilePath), e);
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
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        writeClass(path + packageName, classToWrite.findCompilationUnit().get(), classToWrite);
    }

    public static void writeClass(String path, CompilationUnit compilationUnit, ClassOrInterfaceDeclaration clazz) {
        var javaFilePath = "%s/%s.java".formatted(path, clazz.getName());
        try {
            var javaFile = new File(javaFilePath);
            sortClassMembersAndImports(clazz);
            Files.write(javaFile.toPath(), printer.print(compilationUnit).getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to file %s: %s".formatted(javaFilePath, e.getMessage()), e);
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
            throw new RuntimeException(e);
        }
    }
}
