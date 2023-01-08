package i5.bml.transpiler.generators.types.components;

import generatedParser.BMLParser;
import i5.bml.parser.types.components.BMLOpenAI;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.JavaTreeGenerator;

@CodeGenerator(typeClass = BMLOpenAI.class)
public class OpenAIGenerator implements Generator {

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {

    }
}
