package i5.bml.langserver;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;

public class BMLLanguageServer implements LanguageServer, LanguageClientAware {

    private final TextDocumentService textDocumentService;

    private final WorkspaceService workspaceService;

    private LanguageClient client;

    private int exitCode = 1;

    public BMLLanguageServer() {
        textDocumentService = new BMLTextDocumentService(this);
        workspaceService = new BMLWorkspaceService();
    }

    @Override
    public void initialized(InitializedParams params) {
        LanguageServer.super.initialized(params);
    }

    @Override
    public NotebookDocumentService getNotebookDocumentService() {
        return LanguageServer.super.getNotebookDocumentService();
    }

    @Override
    public void cancelProgress(WorkDoneProgressCancelParams params) {
        LanguageServer.super.cancelProgress(params);
    }

    @Override
    public void setTrace(SetTraceParams params) {
        LanguageServer.super.setTrace(params);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        var initializeResult = new InitializeResult(new ServerCapabilities());

        // Set capabilities of the LS and inform client about them
        var capabilities = initializeResult.getCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setCompletionProvider(new CompletionOptions());
        capabilities.setCodeActionProvider(false);
        capabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions());
        //capabilities.setDocumentHighlightProvider(new DocumentHighlightOptions());
        return CompletableFuture.supplyAsync(() -> initializeResult);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        exitCode = 0;
        return CompletableFuture.supplyAsync(() -> Boolean.TRUE);
    }

    @Override
    public void exit() {
        System.exit(exitCode);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    public LanguageClient getClient() {
        return client;
    }
}
