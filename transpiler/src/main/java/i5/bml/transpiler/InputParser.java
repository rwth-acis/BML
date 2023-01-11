package i5.bml.transpiler;

import i5.bml.parser.Parser;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.utils.IOUtil;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;

public class InputParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputParser.class);

    private String outputDir;

    private String outputPackage;

    public static final String BOT_DIR = "transpiler/src/main/java/i5/bml/transpiler/bot";

    private String inputFilePath;

    private String outputFormat;

    private final IOFileFilter componentDirFilter;

    public InputParser() {
        var componentPackageNames = new String[]{"slack", "telegram", "openapi", "rasa", "dialogue", "openai"};
        var componentNameFilter = FileFilterUtils.or(Arrays.stream(componentPackageNames).map(FileFilterUtils::nameFileFilter).toArray(IOFileFilter[]::new));
        componentDirFilter = FileFilterUtils.notFileFilter(FileFilterUtils.and(componentNameFilter, FileFilterUtils.directoryFileFilter()));
    }

    public void parse(String[] args) throws IOException {
        // Parse options
        var options = initOptions();
        try {
            parseArguments(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("bmlc", options);
            return;
        }

        var start = System.currentTimeMillis();

        // Start processing input file
        var inputString = FileUtils.readFileToString(new File(inputFilePath), Charset.defaultCharset());
        var pair = Measurements.measure("Parsing", () -> Parser.parse(inputString));
        var tree = pair.getRight().program();

        // Collect diagnostics from parse tree
        var diagnosticsCollector = new DiagnosticsCollector();
        Measurements.measure("Type checking", () -> ParseTreeWalker.DEFAULT.walk(diagnosticsCollector, tree));
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
        FileUtils.deleteDirectory(new File(outputDir));

        // Create "project" folders
        new File(outputDir + "/src/main/java/").mkdirs();
        new File(outputDir + "/src/main/resources/").mkdirs();
        new File(outputDir + "/src/test/java/").mkdirs();
        new File(outputDir + "/src/test/resources/").mkdirs();

        // Copy files (from transpiler bot directory) that will be copied regardless of what components are present
        var srcDir = new File(BOT_DIR);
        var destDir = new File(outputDir + "/src/main/java/" + outputPackage);
        Measurements.measure("Copying files", () -> IOUtil.copyFiles(srcDir, destDir, outputPackage , componentDirFilter));

        // Copy build file template
        var gradleTemplate = Files.readString(new File("transpiler/src/main/resources/build_template").toPath(), Charset.defaultCharset());
        FileUtils.copyFile(new File("transpiler/src/main/resources/example_training_data.yml"),
                new File(outputDir + "/src/main/resources/example_training_data.yml"));

        // Replace templates in build.gradle template
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

        // Write back gradle file after templating
        Files.write(new File(outputDir + "/build.gradle").toPath(), gradleFile.render().getBytes());

        // Copy gitignore
        FileUtils.copyFile(new File("transpiler/src/main/resources/gitignore_template"), new File(outputDir + "/.gitignore"));

        // Copy SLF4J SimpleLogger config
        FileUtils.copyFile(new File("transpiler/src/main/resources/simplelogger.properties"), new File(outputDir + "/src/main/resources/simplelogger.properties"));

        var end = System.currentTimeMillis();

        LOGGER.info("Compiling BML code took {} ms", end - start);

        if (outputFormat.equals("jar")) {
            Measurements.measure("Compiling generated Java code to JAR", this::outputJar);
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
                    .setStandardOutput(System.out)
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
