package i5.bml.langserver;

import i5.bml.parser.Parser;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BMLTextDocumentService implements TextDocumentService {

    private final BMLLanguageServer bmlLanguageServer;

    public BMLTextDocumentService(BMLLanguageServer bmlLanguageServer) {
        this.bmlLanguageServer = bmlLanguageServer;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        // Provide completion item.
        return CompletableFuture.supplyAsync(() -> {
            List<CompletionItem> completionItems = new ArrayList<>();
            try {
                // Sample Completion item for sayHello
                CompletionItem completionItem = new CompletionItem();
                // Define the text to be inserted in to the file if the completion item is selected.
                completionItem.setInsertText("Bot(host=\"\", port=\"\") {\n    \n}");
                // Set the label that shows when the completion drop down appears in the Editor.
                completionItem.setLabel("Bot()");
                // Set the completion kind. This is a snippet.
                // That means it replace character which trigger the completion and
                // replace it with what defined in inserted text.
                completionItem.setKind(CompletionItemKind.Snippet);
                // This will set the details for the snippet code which will help user to
                // understand what this completion item is.
                completionItem.setDetail("Create Bot body");

                // Add the sample completion item to the list.
                completionItems.add(completionItem);
            } catch (Exception e) {
                //TODO: Handle the exception.
            }

            // Return the list of completion items.
            return Either.forLeft(completionItems);
        });
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return TextDocumentService.super.resolveCompletionItem(unresolved);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return TextDocumentService.super.hover(params);
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        return TextDocumentService.super.signatureHelp(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declaration(DeclarationParams params) {
        return TextDocumentService.super.declaration(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        return TextDocumentService.super.definition(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(TypeDefinitionParams params) {
        return TextDocumentService.super.typeDefinition(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> implementation(ImplementationParams params) {
        return TextDocumentService.super.implementation(params);
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        return TextDocumentService.super.references(params);
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
        bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "Document Highlight Request: " + params.toString()));
        return TextDocumentService.super.documentHighlight(params);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        return TextDocumentService.super.documentSymbol(params);
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        return TextDocumentService.super.codeAction(params);
    }

    @Override
    public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
        return TextDocumentService.super.resolveCodeAction(unresolved);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        return TextDocumentService.super.codeLens(params);
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        return TextDocumentService.super.resolveCodeLens(unresolved);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        return TextDocumentService.super.formatting(params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        return TextDocumentService.super.rangeFormatting(params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        return TextDocumentService.super.onTypeFormatting(params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        return TextDocumentService.super.rename(params);
    }

    @Override
    public CompletableFuture<LinkedEditingRanges> linkedEditingRange(LinkedEditingRangeParams params) {
        return TextDocumentService.super.linkedEditingRange(params);
    }

    @Override
    public void willSave(WillSaveTextDocumentParams params) {
        TextDocumentService.super.willSave(params);
    }

    @Override
    public CompletableFuture<List<TextEdit>> willSaveWaitUntil(WillSaveTextDocumentParams params) {
        return TextDocumentService.super.willSaveWaitUntil(params);
    }

    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
        return TextDocumentService.super.documentLink(params);
    }

    @Override
    public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
        return TextDocumentService.super.documentLinkResolve(params);
    }

    @Override
    public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
        return TextDocumentService.super.documentColor(params);
    }

    @Override
    public CompletableFuture<List<ColorPresentation>> colorPresentation(ColorPresentationParams params) {
        return TextDocumentService.super.colorPresentation(params);
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        return TextDocumentService.super.foldingRange(params);
    }

    @Override
    public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(PrepareRenameParams params) {
        return TextDocumentService.super.prepareRename(params);
    }

    @Override
    public CompletableFuture<List<TypeHierarchyItem>> prepareTypeHierarchy(TypeHierarchyPrepareParams params) {
        return TextDocumentService.super.prepareTypeHierarchy(params);
    }

    @Override
    public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySupertypes(TypeHierarchySupertypesParams params) {
        return TextDocumentService.super.typeHierarchySupertypes(params);
    }

    @Override
    public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySubtypes(TypeHierarchySubtypesParams params) {
        return TextDocumentService.super.typeHierarchySubtypes(params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyItem>> prepareCallHierarchy(CallHierarchyPrepareParams params) {
        return TextDocumentService.super.prepareCallHierarchy(params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyIncomingCall>> callHierarchyIncomingCalls(CallHierarchyIncomingCallsParams params) {
        return TextDocumentService.super.callHierarchyIncomingCalls(params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyOutgoingCall>> callHierarchyOutgoingCalls(CallHierarchyOutgoingCallsParams params) {
        return TextDocumentService.super.callHierarchyOutgoingCalls(params);
    }

    @Override
    public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
        return TextDocumentService.super.selectionRange(params);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "Semantic Tokens Full Request: " + params.toString()));
        return CompletableFuture.supplyAsync(() -> new SemanticTokens(List.of(1)));
    }

    @Override
    public CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> semanticTokensFullDelta(SemanticTokensDeltaParams params) {
        bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "Semantic Tokens Full Delta Request: " + params.toString()));
        return CompletableFuture.supplyAsync(() -> Either.forLeft(new SemanticTokens(List.of(1, 2))));
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensRange(SemanticTokensRangeParams params) {
        bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "Semantic Tokens Range Request: " + params.toString()));
        return CompletableFuture.supplyAsync(() -> new SemanticTokens(List.of(1, 2, 3)));
    }

    @Override
    public CompletableFuture<List<Moniker>> moniker(MonikerParams params) {
        return TextDocumentService.super.moniker(params);
    }

    @Override
    public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
        return TextDocumentService.super.inlayHint(params);
    }

    @Override
    public CompletableFuture<InlayHint> resolveInlayHint(InlayHint unresolved) {
        return TextDocumentService.super.resolveInlayHint(unresolved);
    }

    @Override
    public CompletableFuture<List<InlineValue>> inlineValue(InlineValueParams params) {
        return TextDocumentService.super.inlineValue(params);
    }

    @Override
    public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
        return TextDocumentService.super.diagnostic(params);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        List<Diagnostic> diagnostics = null;
        try {
            diagnostics = Parser.parseAndCollectDiagnostics(params.getTextDocument().getText(), new StringBuilder());
        } catch (Exception e) {
            bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "PARSING FAILED: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace())));
        }
        if (diagnostics != null) {
            for (Diagnostic diagnostic : diagnostics) {
                diagnostic.getRange().getStart().setLine(diagnostic.getRange().getStart().getLine() - 1);
                diagnostic.getRange().getEnd().setLine(diagnostic.getRange().getEnd().getLine() - 1);
            }
            bmlLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), diagnostics));
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        List<Diagnostic> diagnostics = null;
        try {
            diagnostics = Parser.parseAndCollectDiagnostics(params.getContentChanges().get(0).getText(), new StringBuilder());
        } catch (Exception e) {
            bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "PARSING FAILED: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace())));
        }

        if (diagnostics != null) {
            for (Diagnostic diagnostic : diagnostics) {
                diagnostic.getRange().getStart().setLine(diagnostic.getRange().getStart().getLine() - 1);
                diagnostic.getRange().getEnd().setLine(diagnostic.getRange().getEnd().getLine() - 1);
            }
            bmlLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), diagnostics));
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {

    }
}
