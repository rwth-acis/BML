package i5.bml.langserver;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;

public class BMLLanguageServer implements LanguageServer {

    private TextDocumentService textService;

    private WorkspaceService workspaceService;

    private LanguageClient client;

    public BMLLanguageServer() {
        textService = new BMLTextDocumentService(this);
        workspaceService = new BMLWorkspaceService();
    }

    @Override
    public void initialized(InitializedParams params) {
        LanguageServer.super.initialized(params);
    }

    @Override
    public void initialized() {
        LanguageServer.super.initialized();
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
        return null;
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return null;
    }

    @Override
    public void exit() {

    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return null;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return null;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }
}
