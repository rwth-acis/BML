package i5.bml.transpiler.generators.types;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.Type;
import i5.bml.parser.types.components.primitives.BMLList;
import i5.bml.parser.types.components.primitives.BMLNumber;

public class BMLTypeResolver {

    private BMLTypeResolver() {}

    public static Type resolveBMLTypeToJavaType(org.antlr.symtab.Type type) {
        return switch (type.getName()) {
            case "Number" -> {
                var number = (BMLNumber) type;
                if (number.isFloatingPoint()) {
                    yield StaticJavaParser.parseType("Float");
                } else if (number.isLong()) {
                    yield StaticJavaParser.parseType("Long");
                } else {
                    yield StaticJavaParser.parseType("Integer");
                }
            }
            default -> {
                if (type instanceof BMLList listType) {
                    var listTypeString = "List<";
                    var itemType = resolveBMLTypeToJavaType(listType.getItemType());
                    listTypeString += itemType.asString() + ">";
                    yield StaticJavaParser.parseClassOrInterfaceType(listTypeString);
                } else {
                    yield StaticJavaParser.parseClassOrInterfaceType(type.getName());
                }
            }
        };
    }
}
