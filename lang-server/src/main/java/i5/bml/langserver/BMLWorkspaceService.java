package i5.bml.langserver;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BMLWorkspaceService implements WorkspaceService {
    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        return WorkspaceService.super.executeCommand(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
        return WorkspaceService.super.symbol(params);
    }

    @Override
    public CompletableFuture<WorkspaceSymbol> resolveWorkspaceSymbol(WorkspaceSymbol workspaceSymbol) {
        return WorkspaceService.super.resolveWorkspaceSymbol(workspaceSymbol);
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        WorkspaceService.super.didChangeWorkspaceFolders(params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willCreateFiles(CreateFilesParams params) {
        return WorkspaceService.super.willCreateFiles(params);
    }

    @Override
    public void didCreateFiles(CreateFilesParams params) {
        WorkspaceService.super.didCreateFiles(params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willRenameFiles(RenameFilesParams params) {
        return WorkspaceService.super.willRenameFiles(params);
    }

    @Override
    public void didRenameFiles(RenameFilesParams params) {
        WorkspaceService.super.didRenameFiles(params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willDeleteFiles(DeleteFilesParams params) {
        return WorkspaceService.super.willDeleteFiles(params);
    }

    @Override
    public void didDeleteFiles(DeleteFilesParams params) {
        WorkspaceService.super.didDeleteFiles(params);
    }

    @Override
    public CompletableFuture<WorkspaceDiagnosticReport> diagnostic(WorkspaceDiagnosticParams params) {
        return WorkspaceService.super.diagnostic(params);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {

    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {

    }
}
