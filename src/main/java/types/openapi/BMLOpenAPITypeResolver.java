package types.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.antlr.symtab.Type;
import types.BMLBoolean;
import types.BMLList;
import types.BMLNumeric;
import types.BMLString;

import java.util.HashMap;
import java.util.Map;

/**
 * Source of truth: <a href="https://swagger.io/docs/specification/data-models/data-types/">Swagger data types</a>
 */
public class BMLOpenAPITypeResolver {

    private static final Map<String, Map<String, Type>> componentSupportedFields = new HashMap<>();

    private static void computeComponentFields(OpenAPI openAPI, String componentName) {
        var componentSchema = openAPI.getComponents().getSchemas().get(componentName);

        Map<String, Type> supportedSchemaAccesses = new HashMap<>();
        (((Schema<?>) componentSchema).getProperties()).forEach((fieldName, propertySchema) -> {
            var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(propertySchema,
                    "Property", fieldName);
            Type resolvedType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);
            supportedSchemaAccesses.put(fieldName, resolvedType);
        });
        componentSupportedFields.put(componentName, supportedSchemaAccesses);
    }

    public static Type resolveOpenAPITypeToBMLType(OpenAPI openAPI, String type) {
        if (type.startsWith("array")) {
            var arrayItemType = type.substring("array".length() + 1);
            return new BMLList(resolveOpenAPITypeToBMLType(openAPI, arrayItemType));
        } else if (type.startsWith("object")) {
            // TODO:
            // Nested objects

            // 1. Is it an inline component?

            // 2. No additionalProperties or additionalProperties=true or additionalProperties={} -> dictionary

            return null;
        } else {
            Type resolvedType;
            switch (type) {
                case "string" -> resolvedType = new BMLString();
                case "integer" -> resolvedType = new BMLNumeric(false);
                case "number" -> resolvedType = new BMLNumeric(true);
                case "boolean" -> resolvedType = new BMLBoolean();
                default -> {
                    var supportedFields = componentSupportedFields.get(type);
                    if (supportedFields == null) {
                        computeComponentFields(openAPI, type);
                    }
                    resolvedType = new BMLOpenAPISchema(type, componentSupportedFields.get(type));
                }
            }

            return resolvedType;
        }
    }

    public static String extractOpenAPITypeFromSchema(Schema<?> schema, String object, String objectName) {
        // Resolve type
        // TODO: Handle AnyType and OneOf (i.e., mixed types)
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

        return type + arrayType;
    }
}
