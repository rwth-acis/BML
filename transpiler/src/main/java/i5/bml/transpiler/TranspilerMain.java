package i5.bml.transpiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;
import i5.bml.parser.Parser;
import i5.bml.parser.walker.DiagnosticsCollector;
import i5.bml.transpiler.utils.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class TranspilerMain {

    private static String outputDir;

    private static final String BOT_DIR = "transpiler/src/main/java/i5/bml/transpiler/bot";

    private static String inputFilePath;

    private static String outputFormat;

    public static void main(String[] args) throws IOException {
        // Parse options
        var options = initOptions();
        try {
            parseArguments(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            new HelpFormatter().printHelp("bmlc", options);
            return;
        }

        // Start processing input file
        var inputString = FileUtils.readFileToString(new File(inputFilePath), Charset.defaultCharset());
        var pair = Parser.parse(inputString);
        ParseTree tree = pair.getRight().program();

        // Collect diagnostics from parse tree
        DiagnosticsCollector diagnosticsCollector = new DiagnosticsCollector();
        ParseTreeWalker.DEFAULT.walk(diagnosticsCollector, tree);
        var diagnostics = diagnosticsCollector.getCollectedDiagnostics();

        var containsError = false;
        for (Diagnostic diagnostic : diagnostics) {
            System.err.printf("%s: %s%n", diagnostic.getSeverity().name().toUpperCase(), diagnostic.getMessage());

            if (diagnostic.getSeverity() == DiagnosticSeverity.Error) {
                containsError = true;
            }
        }


        if (!containsError) {
            // Prepare output directory
            FileUtils.copyDirectory(new File(BOT_DIR), new File(outputDir));

            // Emit code into output directory
            new JavaSynthesizer(outputDir).visit(tree);

            if (outputFormat.equals("jar")) {
                // TODO: Compile code, output jar, delete output files except jar
            }
        }
    }

    private static Options initOptions() {
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

    private static void parseArguments(Options options, String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(options, args);
        if (cmd.hasOption("output")) {
            outputDir = cmd.getOptionValue("output");
        } else {
            outputDir = "generated-bot";
        }

        inputFilePath = cmd.getOptionValue("input");

        outputFormat = cmd.getOptionValue("format");
    }


}
