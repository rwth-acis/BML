import org.eclipse.lsp4j.launch.LSPLauncher;

import java.util.concurrent.ExecutionException;

public class BMLLangServerMain {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // start the language server
        startServer();
    }

    private static void startServer() throws ExecutionException, InterruptedException {
        var bmlLangServer = new BMLLanguageServer();
        var launcher = LSPLauncher.createServerLauncher(bmlLangServer, System.in, System.out);
        bmlLangServer.setClient(launcher.getRemoteProxy());
        launcher.startListening().get();
    }
}
