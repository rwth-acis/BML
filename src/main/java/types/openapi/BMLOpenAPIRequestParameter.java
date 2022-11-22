package types.openapi;

import org.antlr.symtab.Type;

public record BMLOpenAPIRequestParameter(String name, Type type) {

    @Override
    public String toString() {
        return "BMLOpenAPIRequestParameter{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
