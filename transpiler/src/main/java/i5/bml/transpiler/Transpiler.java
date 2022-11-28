package i5.bml.transpiler;

import i5.bml.parser.Parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Transpiler {
    public static void main(String[] args) {
        var fileName = "Example.bml";
//        var fileName = "OpenAPIPetStoreWithTelegramExample.bml";
//        var fileName = "ExampleAutomaton.bml";
        var inputString = "";
        try {
            var inputResource = Objects.requireNonNull(Transpiler.class.getClassLoader().getResource(fileName));
            inputString = Files.readString(Paths.get(inputResource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        var stringBuilder = new StringBuilder();
        var diagnostics = Parser.parse(inputString, stringBuilder);
        System.out.println(stringBuilder);
    }
}
