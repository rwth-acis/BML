package i5.bml.langserver;

import i5.bml.parser.Parser;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.functions.BMLFunctionType;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BMLTextDocumentService implements TextDocumentService {

    private final BMLLanguageServer bmlLanguageServer;

    private ParseTree parseTree;

    public BMLTextDocumentService(BMLLanguageServer bmlLanguageServer) {
        this.bmlLanguageServer = bmlLanguageServer;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletionItem> completionItems = new ArrayList<>();
            try {
                if (completionParams.getContext().getTriggerCharacter().equalsIgnoreCase(".")) {
                    var column = completionParams.getPosition().getCharacter() - 2;
                    var pair = Parser.findTerminalNode(parseTree, completionParams.getPosition().getLine() + 1, column);

                    if (pair == null) {
                        System.out.println("Couldn't find anything for request: " + completionParams);
                    } else if (pair.getLeft() == null) {
                        System.out.println("Couldn't find token for request: " + completionParams);
                    } else if (pair.getRight() == null) {
                        System.out.println("Couldn't find scope for token: " + pair.getLeft().getText());
                    } else {
                        var symbol = pair.getRight().resolve(pair.getLeft().getText());
                        if (symbol == null) {
                            System.out.println("Couldn't find resolve symbol " + pair.getLeft().getText() + " for request: " + completionParams);
                        } else {
                            ((AbstractBMLType) ((VariableSymbol) symbol).getType()).getSupportedAccesses().forEach((name, type) -> {
                                CompletionItem completionItem = new CompletionItem();
                                if (type instanceof BMLFunctionType functionType) {
                                    completionItem.setInsertText(name + "()");
                                    completionItem.setLabel(name + "()");
                                    completionItem.setKind(CompletionItemKind.Function);
                                    completionItem.setDetail(functionType.toString());
                                } else {
                                    completionItem.setInsertText(name);
                                    completionItem.setLabel(name);
                                    completionItem.setKind(CompletionItemKind.Field);
                                    completionItem.setDetail(name);
                                }

                                completionItems.add(completionItem);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
//        bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "Hover Request: " + params.toString()));
//        return CompletableFuture.supplyAsync(() -> {
//            var h = new Hover();
//            h.setContents(List.of(Either.forLeft("This is a"), Either.forLeft("hover message")));
//            return h;
//        });
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
            var pair = Parser.parseAndCollectDiagnostics(params.getTextDocument().getText(), new StringBuilder());
            parseTree = pair.getLeft();
            diagnostics = pair.getRight();
        } catch (Exception e) {
            e.printStackTrace(System.out);
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
            var pair = Parser.parseAndCollectDiagnostics(params.getContentChanges().get(0).getText(), new StringBuilder());
            parseTree = pair.getLeft();
            diagnostics = pair.getRight();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            bmlLanguageServer.getClient().logMessage(new MessageParams(MessageType.Info, "PARSING FAILED: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace())));
        }

        if (diagnostics != null) {
            for (Diagnostic diagnostic : diagnostics) {
                diagnostic.getRange().getStart().setLine(diagnostic.getRange().getStart().getLine() - 1);
                diagnostic.getRange().getEnd().setLine(diagnostic.getRange().getEnd().getLine() - 1);
            }
            bmlLanguageServer.getClient().refreshDiagnostics();
            bmlLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), diagnostics));
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // Not interesting
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // Not interesting
    }
}
