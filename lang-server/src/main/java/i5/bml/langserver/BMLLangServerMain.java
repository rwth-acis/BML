package i5.bml.langserver;

import org.eclipse.lsp4j.launch.LSPLauncher;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class BMLLangServerMain {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        LogManager.getLogManager().reset();
        Logger globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        globalLogger.setLevel(Level.OFF);

        var bmlLangServer = new BMLLanguageServer();
        // TODO: Use something else that System.in and .out
        var launcher = LSPLauncher.createServerLauncher(bmlLangServer, System.in, System.out);
        bmlLangServer.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }
}
