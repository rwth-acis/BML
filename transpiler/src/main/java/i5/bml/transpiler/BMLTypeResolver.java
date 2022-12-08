package i5.bml.transpiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import i5.bml.parser.types.BMLNumber;

public class BMLTypeResolver {

    public static Type resolveBMLTypeToJavaType(org.antlr.symtab.Type type) {
        return switch (type.getName()) {
            case "Number" -> ((BMLNumber) type).isFloatingPoint() ? PrimitiveType.doubleType() : PrimitiveType.longType();
            default -> StaticJavaParser.parseClassOrInterfaceType(type.getName());
        };
    }
}
