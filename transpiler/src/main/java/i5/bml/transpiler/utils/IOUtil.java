package i5.bml.transpiler.utils;

import i5.bml.transpiler.InputParser;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    public static void copyDirAndRenameImports(String packageName, JavaTreeGenerator visitor) {
        try {
            var srcDir = new File(visitor.botOutputPath() + visitor.outputPackage() + packageName);
            FileUtils.copyDirectory(new File("%s/%s".formatted(InputParser.BOT_DIR, packageName)), srcDir);
            renameImportsForFilesInDir(srcDir, visitor.outputPackage());
        } catch (Exception e) {
            LOGGER.error("Failed to copy from package {}", packageName, e);
        }
    }

    public static void renameImportsForFilesInDir(File dir, String outputPackage) {
        var files = FileUtils.listFiles(dir, null, true);
        for (File file : files) {
            PrinterUtil.readAndWriteJavaFile(file, file.getName().split("\\.")[0], javaFile -> {
                //noinspection OptionalGetWithoutIsPresent
                var compilationUnit = javaFile.findCompilationUnit().get();

                // Rename package declaration
                //noinspection OptionalGetWithoutIsPresent
                var currentPackage = compilationUnit.getPackageDeclaration().get().getName().asString().replaceFirst("i5.bml.transpiler.bot", "");
                if (currentPackage.isEmpty()) {
                    compilationUnit.removePackageDeclaration();
                } else {
                    if (!outputPackage.isEmpty()) {
                        currentPackage = "%s.%s".formatted(outputPackage, currentPackage);
                    }

                    compilationUnit.setPackageDeclaration(currentPackage.replaceFirst("\\.", ""));
                }

                // Rename imports
                compilationUnit.getImports().stream()
                        .filter(i -> i.getName().asString().startsWith("i5.bml.transpiler.bot"))
                        .forEach(i -> {
                            var importName = i.getName().asString().replaceFirst("i5.bml.transpiler.bot.", "");
                            if (!outputPackage.isEmpty()) {
                                i.setName("%s.%s".formatted(outputPackage, importName));
                            } else {
                                i.setName(importName);
                            }
                        });
            });
        }
    }
}
