package i5.bml.transpiler.generators.dialogue;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import generatedParser.BMLParser;
import i5.bml.parser.types.dialogue.BMLDialogue;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;
import i5.bml.transpiler.bot.dialogue.DialogueFactory;
import i5.bml.transpiler.bot.threads.Session;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@CodeGenerator(typeClass = BMLDialogue.class)
public class DialogueGenerator extends Generator {

    public DialogueGenerator(Type dialogueComponent) {}

    /**
     * Invoked by {@link JavaTreeGenerator#visitBotBody(BMLParser.BotBodyContext)} not, as usual, by
     * {@link JavaTreeGenerator#visitComponent(BMLParser.ComponentContext)}.
     * <p>
     * This method is only called if we have at least one dialogue. We then need a few things in the {@link Session} class:
     * <p><ul>
     * <li> A `dialogues` field to store different dialogues for a session (e.g., different events use different dialogues)
     * <li> We instantiate `dialogues` by calling the {@link DialogueFactory} that returns us a dialogue instance
     *      depending on the message event type
     * <li> Lastly, we add a `toString` method for debug purposes
     * </ul><p>
     *
     * @param ctx always null, since we do not need it
     * @param visitor Instance of the current parse tree visitor
     */
    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        PrinterUtil.readAndWriteClass(visitor.botOutputPath(), Session.class, clazz -> {
            var field = clazz.addField("List<%s>".formatted(DialogueAutomaton.class.getSimpleName()), "dialogues");
            Utils.generateRecordStyleGetter(field, false);

            var assignExpr = StaticJavaParser.parseExpression("dialogues = DialogueFactory.createDialogue(messageEventType)");
            clazz.getConstructors().get(0).getBody().addStatement(assignExpr);

            // Add imports for `DialogueAutomaton`, `DialogueFactory`, and `List`
            //noinspection OptionalGetWithoutIsPresent -> We can assume pressence
            var compilationUnit = clazz.findCompilationUnit().get();
            compilationUnit.addImport(Utils.renameImport(DialogueAutomaton.class, visitor.outputPackage()), false, false);
            compilationUnit.addImport(Utils.renameImport(DialogueFactory.class, visitor.outputPackage()), false, false);
            compilationUnit.addImport(List.class);

            Utils.generateToStringMethod(clazz);
        });
    }

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var dialogueClassName = "%sDialogueAutomaton".formatted(StringUtils.capitalize(object.toString()));
        var dialogueObject = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType(dialogueClassName), new NodeList<>());

        PrinterUtil.readAndWriteClass(visitor.botOutputPath(), DialogueFactory.class, clazz -> {
            //noinspection OptionalGetWithoutIsPresent -> We can assume presence
            var methodBody = clazz.getMethodsByName("createDialogue").get(0).getBody().get().asBlockStmt();

            //noinspection OptionalGetWithoutIsPresent -> We can assume presence
            var switchExpr = (SwitchExpr) methodBody.stream()
                    .filter(n -> n instanceof SwitchExpr)
                    .findAny()
                    .get();

            var parentFunctionDefinition = Utils.findParentContext(ctx, BMLParser.FunctionDefinitionContext.class);
            assert parentFunctionDefinition != null;
            for (String annotation : parentFunctionDefinition.annotations) {
                var newEntries = new NodeList<SwitchEntry>();
                var annotationEnumValue = Utils.pascalCaseToSnakeCase(annotation);

                var existingEntry = switchExpr.getEntries().stream()
                        .filter(e -> e.getLabels().stream().anyMatch(l -> l.toString().equals(annotationEnumValue)))
                        .findAny();

                if (existingEntry.isPresent()) {
                    var listArgs = existingEntry.get().getStatements().get(0).asExpressionStmt().getExpression().asMethodCallExpr().getArguments();
                    if (listArgs.stream().noneMatch(a -> a.asObjectCreationExpr().getType().asString().equals(dialogueClassName))) {
                        listArgs.add(dialogueObject);
                    }
                } else {
                    var labels = new NodeList<Expression>(new NameExpr(annotationEnumValue));
                    var stmts = new NodeList<Statement>(new ExpressionStmt(new MethodCallExpr(new NameExpr("List"), "of", new NodeList<>(dialogueObject))));
                    var switchEntry = new SwitchEntry(labels, SwitchEntry.Type.EXPRESSION, stmts);
                    newEntries.add(switchEntry);
                }

                switchExpr.getEntries().addAll(newEntries);
            }
        });

        // Add import for `ComponentRegistry`
        //noinspection OptionalGetWithoutIsPresent -> We can assume pressence
        var compilationUnit = visitor.currentClass().findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(ComponentRegistry.class, visitor.outputPackage()), false, false);

        var invokeModel = "ComponentRegistry.rasa().invokeModel(ctx.event())";
        var step = "ctx.event().session().dialogues().forEach(d -> d.step(ctx))";
        return new BlockStmt().addStatement(StaticJavaParser.parseExpression(invokeModel)).addStatement(StaticJavaParser.parseExpression(step));
    }
}
