package types;

import generatedParser.BMLParser;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.RuleContext;

import java.lang.annotation.Inherited;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBMLType implements Type, Cloneable {

    protected Map<String, Type> supportedAccesses = new HashMap<>();

    @Override
    public String getName() {
        return this.getClass().getAnnotation(BMLType.class).typeString();
    }

    @Override
    public int getTypeIndex() {
        return this.getClass().getAnnotation(BMLType.class).index();
    }

    public Map<String, Type> getSupportedAccesses() {
        return supportedAccesses;
    }

    @BMLAccessResolver
    public Type resolveAccess(RuleContext ctx) {
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractBMLType that = (AbstractBMLType) o;

        return this.getTypeIndex() == that.getTypeIndex();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Class %s does not implement type cloning".formatted(this.getClass().getName()));
    }
}
