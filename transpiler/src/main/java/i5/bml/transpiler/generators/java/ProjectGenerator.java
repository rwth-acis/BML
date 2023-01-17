package i5.bml.transpiler.generators.java;

import i5.bml.parser.utils.Measurements;
import i5.bml.transpiler.utils.IOUtil;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.FileUtils;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.function.Predicate;

public class ProjectGenerator {

    private final String outputDir;

    private String outputPackage;

    private final String outputFormat;

    public ProjectGenerator(String outputDir, String outputPackage, String outputFormat) {
        this.outputDir = outputDir;
        this.outputPackage = outputPackage;
        this.outputFormat = outputFormat;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectGenerator.class);

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void invokeCodeGeneration(ParseTree tree, long start) throws IOException {
        // Prepare output directory
        outputPackage = outputPackage.replace("\\.", "/");

        IOUtil.deleteDirectory(new File(outputDir + "/src"));

        // Create "project" folders (use mkdirs() to implicitly create parents)
        new File(outputDir + "/src/main/java/").mkdirs();
        new File(outputDir + "/src/main/resources/").mkdirs();
        new File(outputDir + "/src/test/java/").mkdirs();
        new File(outputDir + "/src/test/resources/").mkdirs();

        // Copy files (bot code template) that will be copied regardless of what components are present, hence dirFilter
        var destDir = new File(outputDir + "/src/main/java/" + outputPackage);
        var componentNames = new String[]{"dialogue", "threads/openai", "threads/rasa", "threads/slack", "threads/telegram"};
        Predicate<String> dirFilter = srcFile -> Arrays.stream(componentNames).anyMatch(srcFile::startsWith);
        Measurements.measure("Copying bot template files", () -> IOUtil.copyFiles("bot", destDir, outputPackage, dirFilter));

        // Replace templates in build.gradle template
        var gradleTemplate = IOUtil.getResourceAsString("build_template");
        ST gradleFile = new ST(gradleTemplate);
        gradleFile.add("groupId", outputPackage);
        gradleFile.add("needsDot", outputPackage.isEmpty() ? "" : ".");
        gradleFile.add("mainClass", "BotMain");
        // Set components
        gradleFile.add("hasSlackComponent", false);
        gradleFile.add("hasTelegramComponent", false);
        gradleFile.add("hasOpenAPIComponent", false);
        gradleFile.add("hasRasaComponent", false);
        gradleFile.add("hasOpenAIComponent", false);

        // Emit code into output directory
        Measurements.measure("Code generation", () -> new JavaTreeGenerator(outputDir + "/src/main/java/", outputPackage, gradleFile).visit(tree));

        // Write back gradle file after templating, we do this AFTER the JavaTreeGenerator
        // since it might switch some settings, before rendering
        Files.write(new File(outputDir + "/build.gradle").toPath(), gradleFile.render().getBytes());

        // Copy gitignore
        var gitignoreStream = IOUtil.getResourceAsStream("gitignore_template");
        Files.copy(gitignoreStream, new File(outputDir + "/.gitignore").toPath(), StandardCopyOption.REPLACE_EXISTING);
        gitignoreStream.close();

        // Copy SLF4J SimpleLogger config
        var simpleLoggerStream = IOUtil.getResourceAsStream("simplelogger.properties");
        Files.copy(simpleLoggerStream, new File(outputDir + "/simplelogger.properties").toPath(), StandardCopyOption.REPLACE_EXISTING);
        simpleLoggerStream.close();

        var end = System.nanoTime();

        LOGGER.info("Compiling BML code took %.2f ms".formatted((end - start) / 1_000_000d));

        if (outputFormat.equals("jar")) {
            Measurements.measure("Compiling & packaging generated Java code to JAR", this::outputJar);
        }
    }

    private void outputJar() {
        LOGGER.info("Starting compilation process ...");

        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(new File(outputDir))
                .connect();

        try (connection) {
            connection.newBuild()
                    .forTasks("jar")
                    .setColorOutput(true)
                    .setStandardError(System.err)
                    .run(new ResultHandler<>() {
                        @Override
                        public void onComplete(Void result) {
                            LOGGER.info("Compilation process done");
                        }

                        @Override
                        public void onFailure(GradleConnectionException failure) {
                            LOGGER.info("Compilation process failed", failure);
                        }
                    });
        } catch (IllegalStateException e) {
            LOGGER.error("Compilation process failed", e);
        }
    }
}
