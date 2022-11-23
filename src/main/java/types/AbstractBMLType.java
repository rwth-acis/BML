package types;


import org.antlr.symtab.Type;

public abstract class AbstractBMLType implements Type, Cloneable {

    @Override
    public String getName() {
        return this.getClass().getAnnotation(BMLType.class).typeString();
    }

    @Override
    public int getTypeIndex() {
        return this.getClass().getAnnotation(BMLType.class).index();
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
        throw new CloneNotSupportedException("Class %s does not implement type cloning"
                .formatted(this.getClass().getName()));
    }
}
