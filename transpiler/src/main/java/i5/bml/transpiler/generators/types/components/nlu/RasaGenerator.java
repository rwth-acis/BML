package i5.bml.transpiler.generators.types.components.nlu;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.nlu.BMLRasaComponent;
import i5.bml.transpiler.bot.config.BotConfig;
import i5.bml.transpiler.bot.threads.rasa.RasaComponent;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.generators.types.components.InitializableComponent;
import i5.bml.transpiler.generators.types.components.UsesEnvVariable;
import i5.bml.transpiler.utils.IOUtil;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLRasaComponent.class)
public class RasaGenerator extends Generator implements InitializableComponent, UsesEnvVariable {

    private final BMLRasaComponent rasaComponent;

    private static final String NLU_FALLBACK_INTENT = "nlu_fallback";

    public RasaGenerator(Type rasaComponent) {
        this.rasaComponent = (BMLRasaComponent) rasaComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Make sure that dependencies and included in gradle build file
        visitor.gradleFile().add("hasRasaComponent", true);

        // Copy required implementation for Rasa
        IOUtil.copyDirAndRenameImports("threads/rasa", visitor);

        // Add field
        var type = StaticJavaParser.parseClassOrInterfaceType(RasaComponent.class.getSimpleName());
        var fieldName = "rasa";
        var initializer = new ObjectCreationExpr(null, type,
                new NodeList<>(getFromEnv(rasaComponent.url()), getFromEnv(rasaComponent.trainingFileName())));
        FieldDeclaration field = currentClass.addFieldWithInitializer(type, fieldName,
                initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add getter
        Utils.generateRecordStyleGetter(field, true);

        // Add component initializer method to registry
        var expr = new MethodReferenceExpr(new NameExpr(fieldName), new NodeList<>(), "init");
        addComponentInitializerMethod(currentClass, "Rasa", RasaComponent.class, expr, visitor.outputPackage());

        // Add constant with fallback intent
        PrinterUtil.readAndWriteClass(visitor.botOutputPath(), BotConfig.class, clazz -> {
            var fallbackIntentField = clazz.addFieldWithInitializer(String.class, "NLU_FALLBACK_INTENT", new StringLiteralExpr(NLU_FALLBACK_INTENT));
            fallbackIntentField.setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
        });
    }
}
