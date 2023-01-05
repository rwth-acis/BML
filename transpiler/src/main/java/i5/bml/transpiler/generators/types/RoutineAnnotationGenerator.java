package i5.bml.transpiler.generators.types;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.annotations.BMLRoutineAnnotation;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

import java.util.concurrent.TimeUnit;

@CodeGenerator(typeClass = BMLRoutineAnnotation.class)
public class RoutineAnnotationGenerator implements Generator {

    private static final String PATH = "events/routines";

    private static final String CLASS_NAME = "RoutineEventHandler";

    private final BMLRoutineAnnotation bmlRoutineAnnotation;

    public RoutineAnnotationGenerator(Type bmlRoutineAnnotation) {
        this.bmlRoutineAnnotation = (BMLRoutineAnnotation) bmlRoutineAnnotation;
    }

    @Override
    public void populateClassWithFunction(BMLParser.FunctionDefinitionContext functionContext,
                                          BMLParser.AnnotationContext annotationContext, JavaTreeGenerator visitor) {
        Utils.readAndWriteClass("%s%s".formatted(visitor.botOutputPath(), PATH), CLASS_NAME, clazz -> {
            var handlerMethod = clazz.addMethod(functionContext.head.functionName.getText(), Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
            var annotation = new NormalAnnotationExpr(new Name("RoutineEventHandlerMethod"),
                    new NodeList<>(
                            new MemberValuePair("period", new LongLiteralExpr(bmlRoutineAnnotation.getPeriod())),
                            new MemberValuePair("timeUnit", new FieldAccessExpr(new NameExpr("TimeUnit"), bmlRoutineAnnotation.getTimeUnit().name()))
                    ));
            handlerMethod.addAnnotation(annotation);
            handlerMethod.addParameter("RoutineEventContext", "ctx");
            visitor.classStack().push(clazz);
            handlerMethod.setBody((BlockStmt) visitor.visitFunctionDefinition(functionContext));
            visitor.classStack().pop();

            // Add imports for `MessageEventContext` and `MessageEventHandlerMethod`
            //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
            var compilationUnit = clazz.findCompilationUnit().get();
            compilationUnit.addImport(TimeUnit.class);
            compilationUnit.addImport(Utils.renameImport(TimeUnit.class, visitor.outputPackage()), false, false);
        });
    }
}
