package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.annotations.BMLRoutineAnnotation;
import i5.bml.transpiler.bot.events.routines.RoutineEventContext;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLRoutineAnnotation.class)
public class RoutineAnnotationGenerator implements Generator {

    private static final String PATH = "events/routines";

    private static final String CLASS_NAME = "RoutineEventHandler";

    private ClassOrInterfaceDeclaration clazz;

    private final BMLRoutineAnnotation bmlRoutineAnnotation;

    public RoutineAnnotationGenerator(Type bmlRoutineAnnotation) {
        this.bmlRoutineAnnotation = (BMLRoutineAnnotation) bmlRoutineAnnotation;
    }

    @Override
    public void populateClassWithFunction(String botOutputPath, BMLParser.FunctionDefinitionContext functionContext,
                                          BMLParser.AnnotationContext annotationContext, BMLBaseVisitor<Node> visitor) {
        Utils.readAndWriteClass("%s%s".formatted(botOutputPath, PATH), CLASS_NAME, clazz -> {
            var handlerMethod = clazz.addMethod(functionContext.head.functionName.getText(), Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
            handlerMethod.addParameter(RoutineEventContext.class, "context");
            handlerMethod.setBody((BlockStmt) visitor.visit(functionContext.body));
        });

        // TODO: Set handler in Bot.java
    }
}
