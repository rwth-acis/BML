package types.openapi;

import io.swagger.v3.oas.models.media.Schema;
import org.antlr.symtab.Type;
import types.BMLBoolean;
import types.BMLList;
import types.BMLNumeric;
import types.BMLString;

public class BMLOpenAPITypeResolver {

    public static Type resolveOpenAPITypeToBMLType(String type) {
        if (type.startsWith("array")) {
            var arrayItemType = type.substring("array".length() + 1);
            return new BMLList(resolveOpenAPITypeToBMLType(arrayItemType));
        } else {
            return switch (type) {
                case "string" -> new BMLString();
                case "integer", "number" -> new BMLNumeric();
                case "boolean" -> new BMLBoolean();
                case "object" -> null;
                default -> new BMLOpenAPISchema(type); // Note: We assume type to be a valid schema
            };
        }
    }

    public static String extractOpenAPITypeFromSchema(Schema<?> schema, String object, String objectName) {
        // Resolve type
        String type;
        if (schema.getType() == null) {
            if (schema.get$ref() == null) {
                // TODO
                throw new IllegalStateException("%s %s does not have a type"
                        .formatted(object, objectName));
            }

            var refParts = schema.get$ref().split("/");
            type = refParts[refParts.length - 1];
        } else {
            type = schema.getType();
        }

        // If we have an array, we similarly try to resolve its type
        String arrayType = "";
        if (type.equals("array")) {
            var itemSchema = schema.getItems();
            if (itemSchema == null) {
                // TODO
                throw new IllegalStateException(("%s %s has type array " +
                        "but no specification of its items")
                        .formatted(object, objectName));
            }

            if (itemSchema.getType() == null) {
                if (itemSchema.get$ref() == null) {
                    // TODO
                    throw new IllegalStateException(("%s %s has type array " +
                            "but no type specification for its items")
                            .formatted(object, objectName));
                }

                var refParts = itemSchema.get$ref().split("/");
                arrayType = "." + refParts[refParts.length - 1];
            } else {
                arrayType = "." + itemSchema.getType();
            }
        }

        return type.toLowerCase() + arrayType.toLowerCase();
    }
}
