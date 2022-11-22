package types.openapi;

import org.antlr.symtab.Type;

public record BMLOpenAPIResponse(Type type) {

    // TODO: Implement Trie
    // private Trie responseObjectAccess


    @Override
    public String toString() {
        return "BMLOpenAPIResponse{" +
                "type=" + type +
                '}';
    }
}
