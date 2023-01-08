package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.UnknownType;
import generatedParser.BMLParser;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.dialogue.BMLDialogue;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;
import i5.bml.transpiler.bot.dialogue.DialogueFactory;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@CodeGenerator(typeClass = BMLDialogue.class)
public class DialogueGenerator extends Generator {

    public DialogueGenerator(Type dialogueComponent) {}

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

        var event = new MethodCallExpr(new NameExpr("ctx"), "event");
        var session = new MethodCallExpr(event, "session");
        var dialogues = new MethodCallExpr(session, "dialogues");
        var stepCall = new MethodCallExpr(new NameExpr("d"), "step", new NodeList<>(new NameExpr("ctx")));
        var lambda = new LambdaExpr(new Parameter(new UnknownType(), "d"), stepCall);
        return new MethodCallExpr(dialogues, "forEach", new NodeList<>(lambda));
    }
}
