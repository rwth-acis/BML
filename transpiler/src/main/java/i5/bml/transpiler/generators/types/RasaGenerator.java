package i5.bml.transpiler.generators.types;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLRasaComponent;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLRasaComponent.class)
public class RasaGenerator implements Generator {

    public RasaGenerator(Type rasaType) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaSynthesizer visitor) {}
}
