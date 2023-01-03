package i5.bml.transpiler.generators.types;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import i5.bml.parser.types.BMLNumber;

public class BMLTypeResolver {

    public static Type resolveBMLTypeToJavaType(org.antlr.symtab.Type type) {
        return switch (type.getName()) {
            case "Number" -> {
                var number = (BMLNumber) type;
                if (number.isFloatingPoint()) {
                    yield PrimitiveType.floatType();
                } else if (number.isLong()) {
                    yield PrimitiveType.longType();
                } else {
                    yield PrimitiveType.intType();
                }
            }
            default -> StaticJavaParser.parseClassOrInterfaceType(type.getName());
        };
    }
}
