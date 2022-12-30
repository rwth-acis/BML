package i5.bml.transpiler;

import i5.bml.parser.Parser;
import i5.bml.parser.walker.DiagnosticsCollector;
import i5.bml.transpiler.utils.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class InputParser {

    private String outputDir;

    private String outputPackage;

    private static final String BOT_DIR = "transpiler/src/main/java/i5/bml/transpiler/bot";

    private String inputFilePath;

    private String outputFormat;

    public void parse(String[] args) throws IOException {
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
            System.err.printf("%s line %s: %s%n", diagnostic.getSeverity().name().toUpperCase(),
                    diagnostic.getRange().getStart().getLine(),
                    diagnostic.getMessage());

            if (diagnostic.getSeverity() == DiagnosticSeverity.Error) {
                containsError = true;
            }
        }


        if (!containsError) {
            // Prepare output directory
            outputPackage = outputPackage.replaceAll("\\.", "/");

            // Copies source code to specified directory
            FileUtils.deleteDirectory(new File(outputDir + "/src/main/java/" + outputPackage));
            FileUtils.copyDirectory(new File(BOT_DIR), new File(outputDir + "/src/main/java/" + outputPackage));

            for (File file : FileUtils.listFiles(new File(outputDir + "/src/main/java/" + outputPackage), null, true)) {
                Utils.readAndWriteJavaFile(file, file.getName().split("\\.")[0], javaFile -> {
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

            // Create "project" folders
            new File(outputDir + "/src/main/resources/").mkdir();
            new File(outputDir + "/src/test/java/").mkdir();
            new File(outputDir + "/src/test/resources/").mkdir();

            // Copy build file template
            var gradleTemplate = Files.readString(new File("transpiler/src/main/resources/build_template").toPath(), Charset.defaultCharset());
            FileUtils.copyFile(new File("transpiler/src/main/resources/example_training_data.yml"),
                    new File(outputDir + "/src/main/resources/example_training_data.yml"));

            // Replace templates in build.gradle template
            ST gradleFile = new ST(gradleTemplate);
            gradleFile.add("groupId", outputPackage);
            gradleFile.add("needsDot", outputPackage.isEmpty() ? "" : ".");
            gradleFile.add("mainClass", "BotMain");
            Files.write(new File(outputDir + "/build.gradle").toPath(), gradleFile.render().getBytes());

            // Copy gitignore
            FileUtils.copyFile(new File("transpiler/src/main/resources/gitignore_template"), new File(outputDir + "/.gitignore"));

            // Emit code into output directory
            new JavaSynthesizer(outputDir + "/src/main/java/", outputPackage).visit(tree);

            if (outputFormat.equals("jar")) {
                // TODO: Compile code, output jar, delete output files except jar
            }
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

        var outputPackage = Option.builder("p")
                .longOpt("package")
                .argName("package-names")
                .hasArg()
                .desc("define your package name, e.g., com.example.project")
                .build();
        options.addOption(outputPackage);

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
