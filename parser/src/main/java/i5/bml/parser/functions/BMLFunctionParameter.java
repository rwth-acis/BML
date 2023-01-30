package i5.bml.parser.functions;

import generatedParser.BMLParser;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a parameter for a {@link BMLFunction}.
 */
public class BMLFunctionParameter extends ParameterSymbol {

    /**
     * The context of the expression
     */
    private BMLParser.ExpressionContext exprCtx;

    /**
     * A list of allowed types for this parameter
     */
    private final List<Type> allowedTypes = new ArrayList<>();

    /**
     * Creates a new {@code BMLFunctionParameter} instance with the given name.
     *
     * @param name the name of the parameter
     */
    public BMLFunctionParameter(String name) {
        super(name);
    }

    /**
     * Creates a new {@code BMLFunctionParameter} instance with the given name and type.
     *
     * @param name the name of the parameter
     * @param type the type of the parameter
     */
    public BMLFunctionParameter(String name, Type type) {
        super(name);
        setType(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(Type type) {
        super.setType(type);
        allowedTypes.add(type);
    }

    /**
     * Calls the super method to set the type.
     *
     * @param type the type to set
     */
    public void superSetType(Type type) {
        super.setType(type);
    }

    /**
     * Adds a type to the list of allowed types for this parameter.
     *
     * @param type the type to add
     */
    public void addType(Type type) {
        allowedTypes.add(type);
    }

    /**
     * Gets the {@link BMLParser.ExpressionContext} associated with this parameter.
     *
     * @return the expression context.
     */
    public BMLParser.ExpressionContext exprCtx() {
        return exprCtx;
    }

    /**
     * Sets the {@link BMLParser.ExpressionContext} associated with this parameter.
     *
     * @param exprCtx the expression context to set.
     */
    public void exprCtx(BMLParser.ExpressionContext exprCtx) {
        this.exprCtx = exprCtx;
    }

    /**
     * Gets the list of allowed types for this parameter.
     *
     * @return the list of allowed types.
     */
    public List<Type> allowedTypes() {
        return allowedTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BMLFunctionParameter that = (BMLFunctionParameter) o;

        if (!Objects.equals(exprCtx, that.exprCtx)) return false;
        return allowedTypes.equals(that.allowedTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (exprCtx != null ? exprCtx.hashCode() : 0);
        result = 31 * result + allowedTypes.hashCode();
        return result;
    }
}
