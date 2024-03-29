package i5.bml.transpiler;

import i5.bml.transpiler.input.InputParser;
import i5.bml.transpiler.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TranspilerMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranspilerMain.class);

    public static void main(String[] args) throws IOException {
        if (!Utils.isJavaHomeDefined()) {
            LOGGER.error("JAVA_HOME is not defined!");
            return;
        }

        new InputParser().parse(args);
    }
}
