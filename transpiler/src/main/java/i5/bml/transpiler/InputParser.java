package i5.bml.transpiler;

import i5.bml.parser.Parser;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.utils.IOUtil;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.gradle.tooling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class InputParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputParser.class);

    private String outputDir;

    private String outputPackage;

    private String inputFilePath;

    private String outputFormat;

    public void parse(String[] args) throws IOException {
        // Parse options
        var options = initOptions();
        try {
            parseArguments(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("bmlc", options);
            return;
        }

        var start = System.nanoTime();

        // Start processing input file
        var inputString = FileUtils.readFileToString(new File(inputFilePath), Charset.defaultCharset());
        var pair = Measurements.measure("Parsing", () -> Parser.parse(inputString));
        var tree = pair.getRight().program();

        // Collect diagnostics from parse tree
        var diagnosticsCollector = new DiagnosticsCollector();
        try {
            Measurements.measure("Type checking", () -> ParseTreeWalker.DEFAULT.walk(diagnosticsCollector, tree));
        } catch (Exception e) {
            LOGGER.error("Type checking failed", e);
        }
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        var containsError = false;
        for (var diagnostic : diagnostics) {
            System.err.printf("%s line %s: %s%n", diagnostic.getSeverity().name().toUpperCase(),
                    diagnostic.getRange().getStart().getLine(),
                    diagnostic.getMessage());

            if (diagnostic.getSeverity() == DiagnosticSeverity.Error) {
                containsError = true;
            }
        }

        if (!containsError) {
            invokeCodeGeneration(tree, start);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void invokeCodeGeneration(ParseTree tree, Long start) throws IOException {
        // Prepare output directory
        outputPackage = outputPackage.replace("\\.", "/");

        // Delete old files, in case they exist
        IOUtil.deleteDirectory(new File(outputDir));

        // Create "project" folders
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
        Files.copy(gitignoreStream, new File(outputDir + "/.gitignore").toPath());
        gitignoreStream.close();

        // Copy SLF4J SimpleLogger config
        var simpleLoggerStream = IOUtil.getResourceAsStream("simplelogger.properties");
        Files.copy(simpleLoggerStream, new File(outputDir + "/simplelogger.properties").toPath());
        simpleLoggerStream.close();

        // TODO: Copy training data for Rasa
        //FileUtils.copyFile(new File("transpiler/src/main/resources/example_training_data.yml"), new File(outputDir + "/src/main/resources/example_training_data.yml"));

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

    private Options initOptions() {
        // Instantiate options
        Options options = new Options();
        var inputOption = Option.builder("i")
                .longOpt("input")
                .argName("path")
                .hasArg()
                .required()
                .desc("define input BML file, path can be relative to executable or absolute")
                .build();
        options.addOption(inputOption);

        var outputOption = Option.builder("o")
                .longOpt("output")
                .argName("path")
                .hasArg()
                .desc("define output directory")
                .build();
        options.addOption(outputOption);

        var packageName = Option.builder("p")
                .longOpt("package")
                .argName("package-names")
                .hasArg()
                .desc("define your package name, e.g., com.example.project")
                .build();
        options.addOption(packageName);

        var formatOption = Option.builder("f")
                .longOpt("format")
                .argName("jar|java")
                .hasArg()
                .required()
                .desc("format of the output, either jar or java")
                .build();
        options.addOption(formatOption);

        return options;
    }

    private void parseArguments(Options options, String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(options, args);
        outputDir = cmd.hasOption("output") ? cmd.getOptionValue("output") : "generated-bot";
        outputPackage = cmd.hasOption("package") ? cmd.getOptionValue("package") : "";
        inputFilePath = cmd.getOptionValue("input");
        outputFormat = cmd.getOptionValue("format");
    }
}
