import generatedParser.BMLLexer;
import generatedParser.BMLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import walker.SymbolTableAndScopeGenerator;
import walker.TypeSynthesizer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class BML {
    public static void main(String[] args) {
        var inputString = "";
        try {
            var inputResource = Objects.requireNonNull(BML.class.getClassLoader().getResource("example.bml"));
            //var inputResource = Objects.requireNonNull(BML.class.getClassLoader().getResource("OpenAPIPetStoreWithTelegramExample.bml"));
            inputString = Files.readString(Paths.get(inputResource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

//        ParseOptions parseOptions = new ParseOptions();
//        parseOptions.setFlatten(true);
//        SwaggerParseResult result = new OpenAPIParser().readLocation("https://petstore3.swagger.io/api/v3/openapi.json", null, parseOptions);
//        OpenAPI openAPI = result.getOpenAPI();
//
//        if (result.getMessages() != null) result.getMessages().forEach(System.err::println); // validation errors and warnings
//
//        if (openAPI != null) {
//            //System.out.println(((Schema) openAPI.getComponents().getSchemas().get("Pet").getProperties().get("id")).getType());
//            Schema<?> schema = openAPI.getPaths().get("/pet/findByTags").getGet().getResponses().get("200").getContent().get("application/json").getSchema();
//            System.out.println(schema.getType());
//            System.out.println(schema.getItems());
//        }

        BMLLexer bmlLexer = new BMLLexer(CharStreams.fromString(inputString));
        CommonTokenStream tokens = new CommonTokenStream(bmlLexer);
        BMLParser bmlParser = new BMLParser(tokens);

//        JFrame frame = new JFrame("BML AST");
//        JPanel panel = new JPanel();
//        TreeViewer viewer = new TreeViewer(Arrays.asList(bmlParser.getRuleNames()), bmlParser.program());
//        viewer.setScale(1);
//        panel.add(viewer);
//        frame.add(panel);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.pack();
//        frame.setVisible(true);
//
//        bmlParser.reset();

        ParseTreeWalker walker = new ParseTreeWalker();

        SymbolTableAndScopeGenerator st = new SymbolTableAndScopeGenerator();
        walker.walk(st, bmlParser.program());
        bmlParser.reset();
        TypeSynthesizer typeSynthesizer = new TypeSynthesizer();
        walker.walk(typeSynthesizer, bmlParser.program());
    }
}
