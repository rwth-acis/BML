package i5.bml.transpiler.input;

import i5.bml.parser.Parser;
import i5.bml.parser.errors.SyntaxErrorListener;
import i5.bml.parser.utils.Measurements;
import i5.bml.parser.walker.DiagnosticsCollector;
import i5.bml.transpiler.generators.java.ProjectGenerator;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class InputParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputParser.class);

    private String outputDir;

    private String outputPackage;

    private String inputFilePath;

    private String outputFormat;

    private boolean cachingEnabled;

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
        final String inputString;
        try {
            inputString = FileUtils.readFileToString(new File(inputFilePath), Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("An error occurred while trying to read {}: {}", inputFilePath, e.getMessage());
            LOGGER.debug("Stacktrace:", e);
            return;
        }
        var bmlParser = Measurements.measure("Preparing parser", () -> Parser.bmlParser(inputString));

        var syntaxErrorListener = new SyntaxErrorListener();
        bmlParser.removeErrorListeners();
        bmlParser.addErrorListener(syntaxErrorListener);

        var tree = Measurements.measure("Lexing & Parsing", bmlParser::program);
        var containsError = false;

        // Report syntax errors to stderr
        for (var diagnostic : syntaxErrorListener.getCollectedSyntaxErrors()) {
            System.err.printf("%s line %s: %s%n", diagnostic.getSeverity().name().toUpperCase(),
                    diagnostic.getRange().getStart().getLine(),
                    diagnostic.getMessage());

            if (diagnostic.getSeverity() == DiagnosticSeverity.Error) {
                containsError = true;
            }
        }

        // Even if we encountered a syntax error, ANTLR can recover from it to still provide a semantic analysis, if possible

        // Collect diagnostics from parse tree
        var diagnosticsCollector = new DiagnosticsCollector();
        try {
            Measurements.measure("Semantic analysis", () -> ParseTreeWalker.DEFAULT.walk(diagnosticsCollector, tree));
        } catch (Exception e) {
            LOGGER.error("Semantic analysis failed", e);
            containsError = true;
        }
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        // Report diagnostics to stderr
        for (var diagnostic : diagnostics) {
            System.err.printf("%s line %s: %s%n", diagnostic.getSeverity().name().toUpperCase(),
                    diagnostic.getRange().getStart().getLine(),
                    diagnostic.getMessage());

            if (diagnostic.getSeverity() == DiagnosticSeverity.Error) {
                containsError = true;
            }
        }

        // We only invoke code generation if we did not encounter _errors_
        if (!containsError) {
            new ProjectGenerator(outputDir, outputPackage, outputFormat, cachingEnabled).invokeCodeGeneration(tree, start);
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
                .desc("define output directory for generated code or JAR")
                .build();
        options.addOption(outputOption);

        var packageName = Option.builder("p")
                .longOpt("package")
                .argName("package-names")
                .hasArg()
                .desc("define your package name, e.g., com.example.project, for the code generation (-f java)")
                .build();
        options.addOption(packageName);

        var formatOption = Option.builder("f")
                .longOpt("format")
                .argName("jar|java")
                .hasArg()
                .required()
                .desc("format of the output, either JAR or Java")
                .build();
        options.addOption(formatOption);

        var cacheOption = Option.builder("c")
                .longOpt("cache")
                .desc("allow compiler to maintain Gradle packages in output directory (faster compilation)")
                .build();
        options.addOption(cacheOption);

        return options;
    }

    private void parseArguments(Options options, String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(options, args);
        outputDir = cmd.hasOption("output") ? cmd.getOptionValue("output") : "generated-bot";
        outputPackage = cmd.hasOption("package") ? cmd.getOptionValue("package") : "";
        inputFilePath = cmd.getOptionValue("input");
        outputFormat = cmd.getOptionValue("format");
        cachingEnabled = cmd.hasOption("cache");
    }
}
