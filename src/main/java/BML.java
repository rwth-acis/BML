import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

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
            inputString = Files.readString(Paths.get(inputResource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        BMLLexer bmlLexer = new BMLLexer(CharStreams.fromString(inputString));
        CommonTokenStream tokens = new CommonTokenStream(bmlLexer);
        BMLParser bmlParser = new BMLParser(tokens);

        System.out.println(bmlParser.bot().toStringTree());
    }
}
