package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import i5.bml.transpiler.bot.components.ComponentInitializer;
import i5.bml.transpiler.utils.Utils;

import java.util.concurrent.ExecutorService;

public interface InitializableComponent {

    default void addComponentInitializerMethod(ClassOrInterfaceDeclaration clazz, String componentName, Class<?> threadClass, Expression expr, String outputPackage) {
        // Add initializer method
        var method = clazz.addMethod("init%sComponent".formatted(componentName), Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        method.addAnnotation(new MarkerAnnotationExpr(new Name(ComponentInitializer.class.getSimpleName())));
        method.addParameter(ExecutorService.class, "threadPool");
        method.addParameter(StaticJavaParser.parseType("PriorityBlockingQueue<Event>"), "eventQueue");
        method.setType("CompletableFuture<Void>");
        var runAsync = new MethodCallExpr(new NameExpr("CompletableFuture"), "runAsync", new NodeList<>(expr, new NameExpr("threadPool")));
        method.setBody(new BlockStmt().addStatement(new ReturnStmt(runAsync)));

        // Add import for `threadClass`
        var threadClassImport = Utils.renameImport(threadClass, outputPackage);
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        clazz.findCompilationUnit().get().addImport(threadClassImport, false, false);
    }
}
