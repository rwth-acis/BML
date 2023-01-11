package i5.bml.parser.utils;

import i5.bml.parser.functions.FunctionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    private static final String USER_DIR = System.getProperty("user.dir");

    public static List<Class<?>> collectClassesFromPackage(ClassLoader classLoader, String moduleName, String pathToPackage) {
        var typesDir = new File(USER_DIR + "/" + pathToPackage);
        var packageNames = new ArrayList<String>();
        packageNames.add("i5.bml." + moduleName);
        var classes = new ArrayList<Class<?>>();

        try {
            Files.walkFileTree(typesDir.toPath(), new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    packageNames.add(dir.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var className = String.join(".", packageNames) + "." + file.toFile().getName().split("\\.")[0];
                    try {
                        classes.add(classLoader.loadClass(className));
                    } catch (ClassNotFoundException e) {
                        LOGGER.error("Couldn't load class {}", className, e);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    packageNames.remove(packageNames.size() - 1);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to visit file tree at {}", typesDir, e);
        }

        return classes;
    }
}
