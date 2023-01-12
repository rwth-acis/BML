package i5.bml.parser.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    private IOUtil() {}

    public static List<Class<?>> collectClassesFromJAR(Class<?> caller) {
        var classes = new ArrayList<Class<?>>();
        try (var jarFile = new JarFile(caller.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())) {
            var entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                var je = entries.nextElement();
                var name = je.getName();
                if (je.isDirectory() || !name.endsWith(".class") || name.startsWith("i5")) {
                    continue;
                }

                // -6 because of .class
                String className = name.substring(0, name.length() - 6);
                classes.add(caller.getClassLoader().loadClass(className.replace('/', '.')));
            }
        } catch (ClassNotFoundException | IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return classes;
    }

    public static List<Class<?>> collectClassesFromPackage(String packageName) { return new ArrayList<>();}

    public static void collectClassesFromPackage(Class<?> caller, List<Class<?>> classes, String packageName) {
        var stream = ClassLoader.getSystemClassLoader().getResourceAsStream(packageName.replace('.', '/'));
        if (stream == null) {
            LOGGER.error("Failed to find package {}", packageName);
            return;
        }

        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".class")) {
                    var qualifiedName = packageName + "." + line.substring(0, line.length() - 6);
                    classes.add(caller.getClassLoader().loadClass(qualifiedName));
                } else {
                    collectClassesFromPackage(caller, classes, packageName + "." + line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Class<?>> collectClassesFromPackage(ClassLoader classLoader, String userDir, String moduleName, String pathToPackage) {
        var typesDir = new File(userDir + pathToPackage);
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
