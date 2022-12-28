package i5.bml.transpiler;

import i5.bml.transpiler.utils.Utils;

import java.io.IOException;

public class TranspilerMain {

    public static void main(String[] args) throws IOException {
        if (!Utils.isJavaHomeDefined()) {
            System.err.println("JAVA_HOME is not defined!");
            return;
        }

        new InputParser().parse(args);
    }
}
