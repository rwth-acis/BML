package i5.bml.parser.types.openapi;

import i5.bml.parser.types.BMLList;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.types.TypeRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.antlr.symtab.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Source of truth: <a href="https://swagger.io/docs/specification/data-models/data-types/">Swagger data Types</a>
 */
public class BMLOpenAPITypeResolver {

    private static final String OPENAPI_ARRAY_TYPE_IDENTIFIER = "array";

    private BMLOpenAPITypeResolver() {
    }

    private static void computeComponentFields(OpenAPI openAPI, String componentName, Map<String, Type> supportedFields) {
        var componentSchema = openAPI.getComponents().getSchemas().get(componentName);
        (((Schema<?>) componentSchema).getProperties()).forEach((fieldName, propertySchema) -> {
            var openAPITypeToResolve = BMLOpenAPITypeResolver.extractOpenAPITypeFromSchema(propertySchema,
                    "Property", fieldName);
            Type resolvedType = BMLOpenAPITypeResolver.resolveOpenAPITypeToBMLType(openAPI, openAPITypeToResolve);
            supportedFields.put(fieldName, resolvedType);
        });
    }

    public static Type resolveOpenAPITypeToBMLType(OpenAPI openAPI, String type) {
        if (type.startsWith(OPENAPI_ARRAY_TYPE_IDENTIFIER)) {
            var arrayItemType = type.substring(OPENAPI_ARRAY_TYPE_IDENTIFIER.length() + 1);
            return new BMLList(resolveOpenAPITypeToBMLType(openAPI, arrayItemType));
        } else if (type.startsWith("object")) {
            // TODO:
            // Nested objects

            // 1. Is it an inline component?

            // 2. No additionalProperties or additionalProperties=true or additionalProperties={} -> dictionary

            return null;
        } else {
            return switch (type) {
                case "string", "boolean" -> TypeRegistry.resolveType(type);
                case "int32" -> TypeRegistry.resolveType(BuiltinType.NUMBER);
                case "int64" -> TypeRegistry.resolveType(BuiltinType.LONG_NUMBER);
                case "number" -> TypeRegistry.resolveType(BuiltinType.FLOAT_NUMBER);
                default -> {
                    var resolvedOpenAPIType = TypeRegistry.resolveType(type);
                    if (resolvedOpenAPIType == null) {
                        Map<String, Type> supportedFields = new HashMap<>();
                        computeComponentFields(openAPI, type, supportedFields);

                        // Add to type registry
                        var newType = new BMLOpenAPISchema(type, supportedFields);
                        TypeRegistry.registerType(newType);
                        yield newType;
                    } else {
                        yield resolvedOpenAPIType;
                    }
                }
            };
        }
    }

    public static String extractOpenAPITypeFromSchema(Schema<?> schema, String object, String objectName) {
        // Resolve type
        // TODO: Handle AnyType and OneOf (i.e., mixed types)
        String type;
        if (schema.getType() == null) {
            if (schema.get$ref() == null) {
                // TODO
                throw new IllegalStateException("%s %s does not have a type".formatted(object, objectName));
            }

            var refParts = schema.get$ref().split("/");
            type = refParts[refParts.length - 1];
        } else {
            type = schema.getType();

            // Check for format
            if (schema.getFormat() != null && type.equals("integer")) {
                type = schema.getFormat();
            }
        }

        // If we have an array, we similarly try to resolve its type
        String arrayType = "";
        if (type.equals(OPENAPI_ARRAY_TYPE_IDENTIFIER)) {
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
