import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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

        JFrame frame = new JFrame("BML AST");
        JPanel panel = new JPanel();
        TreeViewer viewer = new TreeViewer(Arrays.asList(bmlParser.getRuleNames()), bmlParser.program());
        viewer.setScale(1.5);
        panel.add(viewer);
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
