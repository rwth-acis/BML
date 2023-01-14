package i5.bml.transpiler.generators.types.components.primitives;

import i5.bml.parser.types.components.primitives.BMLBoolean;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLBoolean.class)
public class BooleanGenerator extends Generator {

    public BooleanGenerator(Type booleanType) {}
}
