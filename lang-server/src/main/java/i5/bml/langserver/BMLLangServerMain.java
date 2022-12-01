package i5.bml.langserver;

import org.eclipse.lsp4j.launch.LSPLauncher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class BMLLangServerMain {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        try (Socket socket = new Socket("localhost", 42069)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            var bmlLangServer = new BMLLanguageServer();
            var launcher = LSPLauncher.createServerLauncher(bmlLangServer, in, out);
            bmlLangServer.connect(launcher.getRemoteProxy());
            launcher.startListening().get();
        }
    }
}
